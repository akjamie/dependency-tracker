#!/usr/bin/env groovy
import groovy.util.Node
import groovy.xml.XmlParser

class MavenDependencyManager {
    static final List<String> SUPPORTED_NAMESPACES = ['http://maven.apache.org/SETTINGS/1.0.0',
            'http://maven.apache.org/SETTINGS/1.1.0',
                                                      'http://maven.apache.org/SETTINGS/1.2.0']

    static final List<String> SUPPORTED_EXTENSIONS_NAMESPACES = ['http://maven.apache.org/EXTENSIONS/1.0.0',
            'http://maven.apache.org/EXTENSIONS/1.1.0',
                                                                 'http://maven.apache.org/EXTENSIONS/1.2.0']

    static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2"
    static final String LOCAL_REPO = System.getProperty('user.home') + '/.m2/repository'

    // Equivalent to PackageDependency in original code
    static class PackageDependency {
        String groupId
        String artifactId
        String currentValue  // version
        String depType
        List<String> registryUrls = []
        String skipReason
        Map<String, Object> props = [:]
        boolean isManaged = false  // Flag to indicate if this is a managed dependency

        String toString() {
            return "${groupId}:${artifactId}:${currentValue}${isManaged ? ' (managed)' : ''}"
        }
    }

    static Node parsePom(String content, String pomFile) {
        try {
            println "[INFO] parsePom, content length: ${content.length()}, pomFile: ${pomFile}"
            def project = new XmlParser().parseText(content)
            println "[INFO] Parsing POM file: ${pomFile}"

            if (content.contains('\r\n')) {
                println "[WARN] POM file contains Windows line endings which may cause issues: ${pomFile}"
            }

            if (project.name().localPart != 'project') {
                println "[ERROR] Not a valid Maven project file: ${pomFile}"
                return null
            }

            // Validate Maven POM version
            if (project.@xmlns == 'http://maven.apache.org/POM/4.0.0' || project.modelVersion.text() == '4.0.0') {
                return project
            }

            println "[ERROR] Unsupported POM version in file: ${pomFile}"
            return null
        } catch (Exception e) {
            println "[ERROR] Failed to parse POM file ${pomFile}: ${e.message}"
            return null
        }
    }

    static Map<String, String> extractProperties(Node project, Map<String, String> inheritedProps = [:]) {
        def props = [:]
        props.putAll(inheritedProps)  // Start with inherited properties

        // Extract properties from current POM
        def propertiesNode = project.properties[0]
        if (propertiesNode) {
            propertiesNode.children().each { prop ->
            if (prop.text()) {
                    // Strip namespace prefix from property name
                    def propName = prop.name().localPart
                    props[propName] = prop.text().trim()
                }
            }
        }
        
        // Extract properties from parent POM if available
        def parent = project.parent[0]
        if (parent) {
            def parentProps = [:]
            def parentPath = parent.relativePath.text()?.trim()
            def parentProject = null

            if (parentPath && parentPath != '') {
                def parentFile = new File(parentPath)
                if (parentFile.exists()) {
                    try {
                        def parentContent = parentFile.text
                        parentProject = parsePom(parentContent, parentPath)
                    } catch (Exception e) {
                        println "[WARN] Could not load parent POM from file: ${e.message}"
                    }
                }
            }

            // If parent POM not found locally, try Maven Central
            if (!parentProject) {
                def groupId = parent.groupId.text()
                def artifactId = parent.artifactId.text()
                def version = parent.version.text()
                parentProject = loadParentPom(groupId, artifactId, version)
            }

            if (parentProject) {
                // Recursively extract properties from parent
                parentProps = extractProperties(parentProject, props)
                props.putAll(parentProps)
            }

            // Extract version properties from parent
            def parentVersion = parent.version.text()?.trim()
            if (parentVersion) {
                props['project.parent.version'] = parentVersion
            }
        }

        // Extract version property from current POM
        def version = project.version.text()?.trim()
        if (version) {
            props['project.version'] = version
        }

        // Add common Maven properties
        props['project.build.sourceEncoding'] = props['project.build.sourceEncoding'] ?: 'UTF-8'
        props['project.reporting.outputEncoding'] = props['project.reporting.outputEncoding'] ?: 'UTF-8'
        
        return props
    }

    static Map<String, PackageDependency> extractManagedDependencies(Node project, Map<String, PackageDependency> inheritedDeps = [:]) {
        def managedDeps = [:]
        def properties = extractProperties(project)

        // First, extract from parent POM to get base managed dependencies
        def parent = project.parent[0]
        if (parent) {
            def parentPath = parent.relativePath.text()?.trim()
            def parentProject = null

            if (parentPath && parentPath != '') {
                def parentFile = new File(parentPath)
                if (parentFile.exists()) {
                    try {
                        def parentContent = parentFile.text
                        parentProject = parsePom(parentContent, parentPath)
                    } catch (Exception e) {
                        println "[WARN] Could not load parent POM from file: ${e.message}"
                    }
                }
            }

            // If parent POM not found locally, try Maven Central
            if (!parentProject) {
                def groupId = parent.groupId.text()
                def artifactId = parent.artifactId.text()
                def version = parent.version.text()
                parentProject = loadParentPom(groupId, artifactId, version)
            }

            if (parentProject) {
                // Recursively extract managed dependencies from parent
                def parentManagedDeps = extractManagedDependencies(parentProject)
                managedDeps.putAll(parentManagedDeps)
            }
        }

        // Then, extract from current POM's dependencyManagement section
        // This will override any versions from parent
        def depMgmt = project.dependencyManagement[0]
        if (depMgmt) {
            println "[DEBUG] Found dependencyManagement section"
            depMgmt.dependencies.dependency.each { dep ->
                def groupId = dep.groupId.text()
                def artifactId = dep.artifactId.text()
                def version = resolveProperty(dep.version.text(), properties)
                def type = dep.type.text() ?: 'jar'
                def scope = dep.scope.text() ?: 'compile'

                // Handle BOM imports
                if (type == 'pom' && scope == 'import') {
                    println "[DEBUG] Found BOM import: ${groupId}:${artifactId}:${version}"
                    def bomProject = loadParentPom(groupId, artifactId, version)
                    if (bomProject) {
                        // Extract properties from BOM
                        def bomProps = extractProperties(bomProject)
                        properties.putAll(bomProps)

                        // Extract managed dependencies from BOM
                        def bomManagedDeps = extractManagedDependencies(bomProject)

                        // Process each managed dependency from BOM
                        bomManagedDeps.each { key, bomDep ->
                            def resolvedVersion = resolveProperty(bomDep.currentValue, properties)
                            if (resolvedVersion && !resolvedVersion.startsWith('${')) {
                                managedDeps[key] = new PackageDependency(groupId: bomDep.groupId,
                                        artifactId: bomDep.artifactId,
                                        currentValue: resolvedVersion,
                                        depType: 'managed',
                                        isManaged: true)
                                println "[DEBUG] Added BOM managed dependency: ${key}:${resolvedVersion}"
                            }
                        }
                    }
                } else if (groupId && artifactId && version) {
                    def key = "${groupId}:${artifactId}"
                    def resolvedVersion = resolveProperty(version, properties)
                    if (resolvedVersion && !resolvedVersion.startsWith('${')) {
                        managedDeps[key] = new PackageDependency(groupId: groupId,
                                artifactId: artifactId,
                                currentValue: resolvedVersion,
                                depType: 'managed',
                                isManaged: true)
                        println "[DEBUG] Found managed dependency: ${key}:${resolvedVersion}"
                    }
                }
            }
        }

        return managedDeps
    }

    static Node loadParentPom(String groupId, String artifactId, String version) {
        if (!version || version.contains('${')) {
            println "[WARN] Cannot load POM with unresolved version: ${groupId}:${artifactId}:${version}"
            return null
        }

        def url = "https://repo.maven.apache.org/maven2/${groupId.replace('.', '/')}/${artifactId}/${version}/${artifactId}-${version}.pom"
        println "[DEBUG] Loading parent POM from Maven Central: ${url}"

        try {
            def connection = new URL(url).openConnection()
            connection.setRequestProperty('User-Agent', 'Maven/3.8.1')
            connection.connectTimeout = 60000
            connection.readTimeout = 60000

            if (connection.responseCode == 200) {
                def inputStream = connection.inputStream
                def content = inputStream.text
                inputStream.close()
                return parsePom(content, "${groupId}:${artifactId}:${version}")
            } else {
                println "[WARN] Could not load parent POM from Maven Central: ${url}"
                return null
            }
        } catch (Exception e) {
            println "[WARN] Error loading parent POM: ${e.message}"
            return null
        }
    }

    static class VersionResolutionContext {
        static final String UNKNOWN_VERSION = "UNKNOWN"
        Map<String, String> properties = [:]
        Map<String, PackageDependency> managedDeps = [:]
        String parentVersion
        boolean isSpringBootProject = false
        String springBootVersion
        Set<String> processedBoms = new HashSet<>()

        VersionResolutionContext(Node project) {
            // Extract properties
            this.properties = extractProperties(project)

            // Check if this is a Spring Boot project
            def parent = project.parent[0]
            if (parent) {
                def parentGroupId = parent.groupId.text()
                def parentArtifactId = parent.artifactId.text()
                this.isSpringBootProject = (parentGroupId == 'org.springframework.boot' && parentArtifactId == 'spring-boot-starter-parent')
                if (this.isSpringBootProject) {
                    this.parentVersion = parent.version.text()
                    this.springBootVersion = this.parentVersion
                }
            }

            // Extract managed dependencies with proper precedence
            this.managedDeps = extractManagedDependencies(project)

            // Process BOM imports
            processBomImports(project)

            // Print managed dependencies for debugging
            println "\n=== Managed Dependencies ==="
            this.managedDeps.each { key, dep -> println "${key}: ${dep.currentValue}"
            }
        }

        void processBomImports(Node project) {
            def depMgmt = project.dependencyManagement[0]
            if (depMgmt) {
                depMgmt.dependencies.dependency.each { dep ->
                    def groupId = dep.groupId.text()
                    def artifactId = dep.artifactId.text()
                    def version = resolveProperty(dep.version.text(), this.properties)
                    def type = dep.type.text() ?: 'jar'
                    def scope = dep.scope.text() ?: 'compile'

                    // Handle BOM imports
                    if (type == 'pom' && scope == 'import') {
                        def bomKey = "${groupId}:${artifactId}:${version}"
                        if (!processedBoms.contains(bomKey)) {
                            processedBoms.add(bomKey)
                            println "[DEBUG] Processing BOM: ${bomKey}"

                            def bomProject = loadParentPom(groupId, artifactId, version)
                            if (bomProject) {
                                // Extract properties from BOM
                                def bomProps = extractProperties(bomProject)
                                this.properties.putAll(bomProps)

                                // Extract managed dependencies from BOM
                                // BOM dependencies should override existing versions
                                def bomManagedDeps = extractManagedDependencies(bomProject)
                                this.managedDeps.putAll(bomManagedDeps)

                                // Recursively process nested BOMs
                                processBomImports(bomProject)
                            } else {
                                println "[WARN] Could not load BOM: ${bomKey}"
                            }
                        }
                    }
                }
            }
        }

        String resolveVersion(String groupId, String artifactId, String explicitVersion) {
            def managedKey = "${groupId}:${artifactId}"

            // 1. Use explicit version if provided
            if (explicitVersion) {
                def resolvedVersion = resolveProperty(explicitVersion, this.properties)
                if (resolvedVersion && !resolvedVersion.startsWith('${')) {
                    println "[DEBUG] Using explicit version for ${managedKey}: ${resolvedVersion}"
                    return resolvedVersion
                }
            }

            // 2. Check dependencyManagement in current POM and imported BOMs
            def managedDep = this.managedDeps[managedKey]
            if (managedDep?.currentValue) {
                def resolvedVersion = resolveProperty(managedDep.currentValue, this.properties)
                if (resolvedVersion && !resolvedVersion.startsWith('${')) {
                    println "[DEBUG] Using managed version for ${managedKey}: ${resolvedVersion}"
                    return resolvedVersion
                }
            }

            // 3. Special handling for Spring Boot artifacts
            if (this.isSpringBootProject && groupId == 'org.springframework.boot') {
                println "[DEBUG] Using Spring Boot parent version for ${managedKey}: ${this.springBootVersion}"
                return this.springBootVersion
            }

            // 4. Special handling for Spring Cloud artifacts
            if (groupId == 'org.springframework.cloud') {
                def springCloudVersion = this.properties['spring-cloud.version']
                if (springCloudVersion) {
                    println "[DEBUG] Using Spring Cloud version for ${managedKey}: ${springCloudVersion}"
                    return springCloudVersion
                }
            }

            // 5. Check for version properties
            def versionProps = ["${groupId}.version",
                                "${artifactId}.version",
                                "${groupId}.${artifactId}.version"]

            for (prop in versionProps) {
                def version = this.properties[prop]
                if (version) {
                    println "[DEBUG] Using property version for ${managedKey}: ${version}"
                    return version
                }
            }

            // 6. Fallback to UNKNOWN if no version can be determined
            println "[WARN] Could not determine version for ${groupId}:${artifactId}, using UNKNOWN"
            return UNKNOWN_VERSION
        }
    }

    static List<PackageDependency> extractDependencies(Node project, boolean isRoot = true) {
        // First try using Maven command line if available
        if (isRoot && isMavenAvailable()) {
            def pomFile = project.@file ?: 'pom.xml'
            def mavenDeps = extractDependenciesUsingMaven(pomFile)
            if (mavenDeps) {
                println "[INFO] Successfully extracted dependencies using Maven command line"
                return mavenDeps
            }
            println "[INFO] Falling back to POM parsing method"
        }

        // Original POM parsing logic as fallback
        def dependencies = []
        def context = new VersionResolutionContext(project)

        // Print extracted properties for debugging
        println "\n=== Extracted Properties ==="
        context.properties.each { k, v -> println "${k} = ${v}" }

        // Extract direct dependencies from current POM only
        def depsNode = project.dependencies[0]
        if (depsNode) {
            println "\n=== Processing Direct Dependencies ==="
            depsNode.dependency.each { dep ->
                def groupId = resolveProperty(dep.groupId.text(), context.properties)
                def artifactId = resolveProperty(dep.artifactId.text(), context.properties)
                def explicitVersion = dep.version.text()
                def scope = dep.scope.text() ?: 'compile'
                
                println "[DEBUG] Processing dependency: ${groupId}:${artifactId}"
                println "[DEBUG] Explicit version from POM: ${explicitVersion}"

                // Resolve version using the context
                def version = context.resolveVersion(groupId, artifactId, explicitVersion)

                def dependency = new PackageDependency(groupId: groupId,
                    artifactId: artifactId,
                    currentValue: version,
                    depType: scope)

                // Handle optional dependencies
                if (dep.optional.text() == 'true') {
                    dependency.depType = 'optional'
                }

                if (dependency.groupId && dependency.artifactId) {
                    println "[INFO] Found dependency: ${dependency}"
                    dependencies << dependency
                } else {
                    println "[WARN] Skipping dependency due to missing groupId or artifactId: ${groupId}:${artifactId}"
                }
            }
        }

        // Extract plugins from current POM only
        def buildNode = project.build[0]
        if (buildNode) {
            def pluginsNode = buildNode.plugins[0]
            if (pluginsNode) {
                pluginsNode.plugin.each { plugin ->
                    def groupId = resolveProperty(plugin.groupId.text() ?: 'org.apache.maven.plugins', context.properties)
                    def artifactId = resolveProperty(plugin.artifactId.text(), context.properties)
                    def explicitVersion = plugin.version.text()

                    // Resolve version using the context
                    def version = context.resolveVersion(groupId, artifactId, explicitVersion)

                    def dependency = new PackageDependency(groupId: groupId,
                        artifactId: artifactId,
                        currentValue: version,
                        depType: 'build')

                    if (dependency.artifactId) {
                        println "[INFO] Found plugin: ${dependency}"
                        dependencies << dependency
                    }
                }
            }
        }
        
        return dependencies
    }


    // Equivalent to MavenInterimPackageFile
    static class PackageFile {
        String datasource
        String packageFile
        List<PackageDependency> deps = []
        Map<String, Object> mavenProps
        String parent
        String packageFileVersion
    }


    static List<String> extractRegistries(String rawContent) {
        if (!rawContent) {
            return []
        }

        def settings = parseSettings(rawContent)
        if (!settings) {
            return []
        }

        def urls = []

        // Extract mirror URLs
        def mirrorUrls = parseUrls(settings, 'mirrors')
        urls.addAll(mirrorUrls)

        // Extract repository URLs from profiles
        settings.profiles.profile.each { profile ->
            def repositoryUrls = parseUrls(profile, 'repositories')
            urls.addAll(repositoryUrls)
        }

        println "[DEBUG] Found registry URLs: ${urls}"
        return urls.unique()
    }

    static List<String> parseUrls(Node xmlNode, String path) {
        def urls = []
        def children = xmlNode.depthFirst().find { it.name() == path }
        if (children) {
            children.children().each { child ->
                def url = child.url?.text()
                if (url) {
                    urls << url
                    println "[DEBUG] Parsed URL: ${url}"
                }
            }
        }
        return urls
    }

    static Node parseSettings(String raw) {
        try {
            def settings = new XmlParser().parseText(raw)
            if (settings.name() != 'settings') {
                println "[DEBUG] Not a settings file"
                return null
            }
            if (SUPPORTED_NAMESPACES.contains(settings.@xmlns)) {
                return settings
            }
            println "[DEBUG] Unsupported namespace in settings file"
            return null
        } catch (Exception e) {
            println "[ERROR] Failed to parse settings: ${e.message}"
            return null
        }
    }


    static String resolveProperty(String value, Map<String, String> properties) {
        if (!value) return value
        if (!value.contains('${')) return value

        def result = value
        def maxIterations = 10  // Prevent infinite recursion
        def iterations = 0
        def processedProps = new HashSet<String>()

        while (result.contains('${') && iterations < maxIterations) {
            def matcher = result =~ /\$\{([^}]+)\}/
            if (!matcher.find()) break

            def propName = matcher.group(1)
            if (processedProps.contains(propName)) {
                println "[WARN] Circular property reference detected: ${propName}"
                break
            }
            processedProps.add(propName)

            def propValue = properties[propName]
            if (propValue) {
                // Handle nested properties in the property value
                if (propValue.contains('${')) {
                    propValue = resolveProperty(propValue, properties)
                }
                result = result.replace('${' + propName + '}', propValue)
            } else {
                // Try to resolve using common property patterns
                def resolved = false

                // Handle version properties
                if (propName.endsWith('.version')) {
                    def artifactId = propName - '.version'
                    def version = properties[artifactId]
                    if (version) {
                        result = result.replace('${' + propName + '}', version)
                        resolved = true
                    }
                }

                // Handle Spring Boot version properties
                if (propName.startsWith('spring-boot.version')) {
                    def version = properties['spring-boot.version']
                    if (version) {
                        result = result.replace('${' + propName + '}', version)
                        resolved = true
                    }
                }

                // Handle Spring Cloud version properties
                if (propName.startsWith('spring-cloud.version')) {
                    def version = properties['spring-cloud.version']
                    if (version) {
                        result = result.replace('${' + propName + '}', version)
                        resolved = true
                    }
                }

                if (!resolved) {
                    println "[WARN] Could not resolve property: ${propName}"
                    break
                }
            }
            iterations++
        }

        if (iterations >= maxIterations) {
            println "[WARN] Maximum property resolution iterations reached for: ${value}"
        }

        return result
    }

    static void uploadDependency(String compiler, String runtimeVersion, String componentId, String branch, String sourceCodeUrl, List<PackageDependency> dependencies) {
        // Get API URL from environment variable, fail if not set
        println "::debug::Pushing dependencies to Dependency Tracker API"
        def apiUrl = System.getenv('DEPENDENCY_TRACKER_API_URL')
        if (!apiUrl) {
            println "::warn::DEPENDENCY_TRACKER_API_URL environment variable is not set, will try the default url."
            apiUrl = "http://localhost:8080/dependency-tracker/api/v1/components"
        }

        // Get API token from environment variable, fail if not set
        def apiToken = System.getenv('DEPENDENCY_TRACKER_API_TOKEN')
        if (!apiToken) {
            println "::warn::DEPENDENCY_TRACKER_API_TOKEN environment variable is not set"
            apiToken = ""
        }

        // Extract name from sourceCodeUrl
        def name = extractNameFromSourceCodeUrl(sourceCodeUrl)


        // Prepare the request payload
        def payload = [
                component   : [
                        name         : name,
                        sourceCodeUrl: sourceCodeUrl,
                        eimId        : '',
                ],
                componentId : componentId,
                branch      : branch,
                compiler    : compiler,
                runtimeInfo  : [
                        version: runtimeVersion,
                        type   : 'JDK',
                ],
                language: 'JAVA',
                buildManager: 'MAVEN',
                dependencies: dependencies.collect { dep ->
                    [
                            artefact: "${dep.groupId}:${dep.artifactId}",
                            version : dep.currentValue,
                            type: dep.depType,
                    ]
                }
        ]

        def maxRetries = 3
        def retryCount = 0
        def success = false

        while (!success && retryCount < maxRetries) {
            try {
                def connection = new URL(apiUrl).openConnection() as HttpURLConnection
                connection.setRequestMethod('PUT')
                connection.setRequestProperty('Content-Type', 'application/json')
                connection.setRequestProperty('Accept', 'application/json')
                connection.setRequestProperty('Authorization', "Bearer ${apiToken}")
                connection.setDoOutput(true)
                connection.setConnectTimeout(10000)
                connection.setReadTimeout(10000)

                // Write the payload
                connection.outputStream.withWriter { writer ->
                    writer << new groovy.json.JsonBuilder(payload).toString()
                }

                // Get the response
                def responseCode = connection.responseCode
                def responseBody = connection.inputStream.text

                if (responseCode == 200) {
                    println "::notice::Successfully pushed dependencies for ${componentId}"
                    if (System.getenv('CI')) {
                        println "::set-output name=status::success"
                        println "::set-output name=componentId::${componentId}"
                    }
                    success = true
                } else if (responseCode == 429) { // Rate limit
                    println "::warning::Rate limited, retrying... (${retryCount + 1}/${maxRetries})"
                    Thread.sleep(1000 * (retryCount + 1))
                    retryCount++
                } else {
                    def errorMsg = "Failed to push dependencies. Status: ${responseCode}, Response: ${responseBody}"
                    println "::error::${errorMsg}"
                    if (System.getenv('CI')) {
                        println "::set-output name=status::failure"
                        println "::set-output name=error::${errorMsg}"
                    }
                    success = true // Don't retry on other errors
                }
            } catch (Exception e) {
                if (retryCount < maxRetries - 1) {
                    println "::warning::Request failed, retrying... (${retryCount + 1}/${maxRetries})"
                    Thread.sleep(1000 * (retryCount + 1))
                    retryCount++
                } else {
                    def errorMsg = "Failed to push dependencies: ${e.message}"
                    println "::error::${errorMsg}"
                    if (System.getenv('CI')) {
                        println "::set-output name=status::failure"
                        println "::set-output name=error::${errorMsg}"
                    }
                    //System.exit(1)
                    retryCount++
                }
            }
        }
    }

    static String extractComponentId(Node project) {
        // Get groupId (can be inherited from parent)
        def groupId = project.groupId.text() ?: project.parent.groupId.text()

        // Get artifactId (must be defined in current POM)
        def artifactId = project.artifactId.text()

        if (!groupId || !artifactId) {
            println "[ERROR] Missing required groupId or artifactId in POM"
            return null
        }

        // Return in format groupId:artifactId
        return "${groupId}:${artifactId}"
    }

    static String extractNameFromSourceCodeUrl(String sourceCodeUrl) {
        try {
            // Handle common Git repository URL patterns
            if (sourceCodeUrl.contains('github.com')) {
                return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
            } else if (sourceCodeUrl.contains('gitlab.com')) {
                return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
            } else if (sourceCodeUrl.contains('bitbucket.org')) {
                return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
            }
            // Default: use the last part of the URL
            return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
        } catch (Exception e) {
            println "[WARN] Failed to extract name from sourceCodeUrl: ${sourceCodeUrl}"
            return 'unknown-component'
        }
    }

    static String extractRuntimeVersion(Node project) {
        def version = null
        
        // 1. Check maven.compiler.source/target properties
        def properties = project.properties[0]
        if (properties) {
            version = properties.'maven.compiler.source'.text() ?: 
                    properties.'maven.compiler.target'.text() ?:
                    properties.'java.version'.text()
        }
        
        // 2. Check maven-compiler-plugin configuration
        if (!version) {
            project.build.plugins.plugin.each { plugin ->
                if (plugin.groupId.text() == 'org.apache.maven.plugins' && 
                    plugin.artifactId.text() == 'maven-compiler-plugin') {
                    def config = plugin.configuration[0]
                    if (config) {
                        version = config.source.text() ?: config.target.text()
                    }
                }
            }
        }
        
        // 3. Check for Spring Boot parent
        if (!version) {
            def parent = project.parent[0]
            if (parent && 
                parent.groupId.text() == 'org.springframework.boot' && 
                parent.artifactId.text() == 'spring-boot-starter-parent') {
                // Spring Boot 3.x requires Java 17+
                if (parent.version.text().startsWith('3.')) {
                    version = '17'
                }
                // Spring Boot 2.x works with Java 8+
                else if (parent.version.text().startsWith('2.')) {
                    version = '8'
                }
            }
        }
        
        // 4. Check for Java version in properties
        if (!version) {
            def javaVersion = project.properties.'java.version'.text()
            if (javaVersion) {
                version = javaVersion
            }
        }
        
        // Normalize version format
        if (version) {
            // Remove any '1.' prefix for Java 8
            version = version.replaceAll('^1\\.', '')
            // Remove any non-numeric characters
            version = version.replaceAll('[^0-9]', '')
            // Add 'JDK ' prefix
            return "${version}"
        }
        
        println "[WARN] Could not determine Java version from POM"
        return "UNKNOWN"
    }

    static boolean isMavenAvailable() {
        try {
            // Use cmd.exe /c for Windows command execution
            def command = System.getProperty('os.name').toLowerCase().contains('windows') ? 
                ['cmd', '/c', 'mvn', '-v'] as String[] : 
                ['mvn', '-v'] as String[]
            
            def process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            def output = new StringBuilder()
            process.inputStream.eachLine { line ->
                output.append(line).append('\n')
            }
            
            def exitCode = process.waitFor()
            println "[DEBUG] Maven version output: ${output.toString()}"
            return exitCode == 0
        } catch (Exception e) {
            println "[DEBUG] Maven command not available: ${e.message}"
            return false
        }
    }

    static List<PackageDependency> extractDependenciesUsingMaven(String pomFile) {
        println "[INFO] Using Maven command line to extract dependencies"
        def dependencies = []
        def tempFile = File.createTempFile("maven-deps-", ".txt")
        
        try {
            // Use cmd.exe /c for Windows command execution
            def command = System.getProperty('os.name').toLowerCase().contains('windows') ? 
                ['cmd', '/c', 'mvn', 'dependency:tree', '-f', pomFile, "-DoutputFile=${tempFile.absolutePath}", '-DoutputType=text'] as String[] : 
                ['mvn', 'dependency:tree', '-f', pomFile, "-DoutputFile=${tempFile.absolutePath}", '-DoutputType=text'] as String[]
            
            def process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            
            def output = new StringBuilder()
            process.inputStream.eachLine { line ->
                output.append(line).append('\n')
            }
            
            def exitCode = process.waitFor()
            println "[DEBUG] Maven dependency:tree output: ${output.toString()}"
            
            if (exitCode != 0) {
                println "[ERROR] Failed to execute mvn dependency:tree"
                return []
            }

            // Parse the output file
            tempFile.eachLine { line ->
                println "[DEBUG] Processing line: ${line}"
                if (line.contains("+-") || line.contains("\\-")) {
                    def dep = parseMavenDependencyLine(line)
                    if (dep) {
                        dependencies << dep
                    }
                }
            }
        } catch (Exception e) {
            println "[ERROR] Failed to extract dependencies using Maven: ${e.message}"
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }
        println "[DEBUG] Extracted dependencies using mvn dependency:tree: ${dependencies}"
        return dependencies
    }

    static PackageDependency parseMavenDependencyLine(String line) {
        try {
            // Remove tree characters and trim
            def cleanLine = line.replaceAll(/[+\\| ]/, '').trim()
            
            // Skip empty lines or lines without version
            if (!cleanLine.contains(':')) return null
            
            // Parse GAV (GroupId:ArtifactId:Type:Version:Scope)
            def parts = cleanLine.split(':')
            if (parts.length < 3) return null
            
            def groupId = parts[0]
            def artifactId = parts[1]
            def type = parts.length > 2 ? parts[2] : 'jar'
            def version = parts.length > 3 ? parts[3] : 'UNKNOWN'
            def scope = parts.length > 4 ? parts[4] : 'compile'
            
            // Determine dependency type
            def depType = scope
            if (scope == 'test') {
                depType = 'test'
            } else if (scope == 'provided') {
                depType = 'provided'
            } else if (scope == 'runtime') {
                depType = 'runtime'
            } else if (scope == 'system') {
                depType = 'system'
            } else if (scope == 'import') {
                depType = 'import'
            } else {
                depType = 'compile'
            }
            
            println "[DEBUG] Parsed dependency: ${groupId}:${artifactId}:${version} (${depType})"
            
            return new PackageDependency(
                groupId: groupId,
                artifactId: artifactId,
                currentValue: version,
                depType: depType
            )
        } catch (Exception e) {
            println "[WARN] Failed to parse dependency line: ${line}"
            e.printStackTrace()
            return null
        }
    }
}

// Main method for testing
static void main(String[] args) {
    if (args.length == 0) {
        println """
        Usage: groovy MavenDependencyExtractor.groovy <pom.xml path>
        Example: groovy MavenDependencyExtractor.groovy pom.xml
        """
        System.exit(1)
    }

    def pomFile = new File(args[0])
    if (!pomFile.exists()) {
        println "[ERROR] POM file not found: ${pomFile}"
        System.exit(1)
    }

    def branch = args[1]
    def sourceCodeUrl = args[2]
    def compiler = args[3]

    try {
        def content = pomFile.text
        def project = MavenDependencyManager.parsePom(content, pomFile.path)
        if (!project) {
            println "[ERROR] Failed to parse POM file: ${pomFile.path}"
            System.exit(1)
        }

        def componentId = MavenDependencyManager.extractComponentId(project)

        def runtimeVersion = MavenDependencyManager.extractRuntimeVersion(project)

        def dependencies = MavenDependencyManager.extractDependencies(project, false)

        MavenDependencyManager.uploadDependency(compiler, runtimeVersion, componentId, branch, sourceCodeUrl, dependencies)

        // Group dependencies by type for clear output
        def grouped = dependencies.groupBy { it.depType }

        println "\n=== Detailed Dependencies ==="
        grouped.each { type, deps ->
            println "\n${type.toUpperCase()}:"
            deps.each { dep -> println "- ${dep}"
            }
        }
    } catch (Exception e) {
        println "[ERROR] Failed to process POM: ${e.message}"
        e.printStackTrace()
        System.exit(1)
    }
}