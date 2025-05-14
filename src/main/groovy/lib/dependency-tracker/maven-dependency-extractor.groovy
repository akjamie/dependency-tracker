#!/usr/bin/env groovy
import groovy.util.Node
import groovy.xml.XmlParser

class MavenDependencyManager {
    static final List<String> SUPPORTED_NAMESPACES = [
            'http://maven.apache.org/SETTINGS/1.0.0',
            'http://maven.apache.org/SETTINGS/1.1.0',
            'http://maven.apache.org/SETTINGS/1.2.0'
    ]

    static final List<String> SUPPORTED_EXTENSIONS_NAMESPACES = [
            'http://maven.apache.org/EXTENSIONS/1.0.0',
            'http://maven.apache.org/EXTENSIONS/1.1.0',
            'http://maven.apache.org/EXTENSIONS/1.2.0'
    ]

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

            // print content
            //println "[DEBUG] POM content: ${content}"
            //println "[DEBUG] project: ${project.name().localPart}"

            if (project.name().localPart != 'project') {
                println "[ERROR] Not a valid Maven project file: ${pomFile}"
                return null
            }

            // Validate Maven POM version
            if (project.@xmlns == 'http://maven.apache.org/POM/4.0.0' ||
                    project.modelVersion.text() == '4.0.0') {
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
        managedDeps.putAll(inheritedDeps)  // Start with inherited managed dependencies
        
        // Extract from dependencyManagement section
        def depMgmt = project.dependencyManagement[0]
        if (depMgmt) {
            println "[DEBUG] Found dependencyManagement section"
            depMgmt.dependencies.dependency.each { dep ->
                def groupId = dep.groupId.text()
                def artifactId = dep.artifactId.text()
                def version = dep.version.text()
                
                if (groupId && artifactId && version) {
                    def key = "${groupId}:${artifactId}"
                    managedDeps[key] = new PackageDependency(
                        groupId: groupId,
                        artifactId: artifactId,
                        currentValue: version,
                        depType: 'managed',
                        isManaged: true
                    )
                    println "[DEBUG] Found managed dependency: ${key}:${version}"
                }
            }
        }
        
        // Handle parent POM
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
                def parentManagedDeps = extractManagedDependencies(parentProject, managedDeps)
                managedDeps.putAll(parentManagedDeps)
            }
        }
        
        return managedDeps
    }

    static Node loadParentPom(String groupId, String artifactId, String version) {
        // First try local repository
        def localPath = "${LOCAL_REPO}/${groupId.replace('.', '/')}/${artifactId}/${version}/${artifactId}-${version}.pom"
        def localFile = new File(localPath)
        if (localFile.exists()) {
            println "[DEBUG] Loading parent POM from local repository: ${localPath}"
            try {
                def content = localFile.text
                return parsePom(content, localPath)
            } catch (Exception e) {
                println "[WARN] Could not load parent POM from local repository: ${e.message}"
            }
        }

        // If not found locally, try Maven Central
        def url = "${MAVEN_CENTRAL}/${groupId.replace('.', '/')}/${artifactId}/${version}/${artifactId}-${version}.pom"
        println "[DEBUG] Loading parent POM from Maven Central: ${url}"
        try {
            def connection = new URL(url).openConnection()
            connection.setRequestProperty('User-Agent', 'Maven-Dependency-Extractor')
            def content = connection.inputStream.text
            return parsePom(content, "${groupId}:${artifactId}:${version}")
        } catch (Exception e) {
            println "[WARN] Could not load parent POM from Maven Central: ${e.message}"
            return null
        }
    }

    static List<PackageDependency> extractDependencies(Node project, boolean isRoot = true) {
        def dependencies = []
        def props = extractProperties(project)
        def managedDeps = extractManagedDependencies(project)

        // Print extracted properties for debugging
        println "\n=== Extracted Properties ==="
        props.each { k, v -> println "${k} = ${v}" }

        // Extract direct dependencies from current POM only
        def depsNode = project.dependencies[0]
        if (depsNode) {
            println "\n=== Processing Direct Dependencies ==="
            depsNode.dependency.each { dep ->
                def groupId = resolveProperty(dep.groupId.text(), props)
                def artifactId = resolveProperty(dep.artifactId.text(), props)
                def version = dep.version.text() ? resolveProperty(dep.version.text(), props) : null
                def scope = dep.scope.text() ?: 'compile'
                
                println "[DEBUG] Processing dependency: ${groupId}:${artifactId}"
                println "[DEBUG] Version from POM: ${version}"
                
                // Check if this is a Spring Boot artifact and version is not specified
                if (groupId == 'org.springframework.boot' && !version) {
                    // Use parent version for Spring Boot artifacts
                    version = props['project.parent.version']
                    println "[INFO] Using parent version ${version} for Spring Boot dependency: ${artifactId}"
                }
                
                // Check if this dependency is managed
                def managedKey = "${groupId}:${artifactId}"
                def managedDep = managedDeps[managedKey]
                if (managedDep) {
                    println "[DEBUG] Found managed version: ${managedDep.currentValue}"
                    version = version ?: managedDep.currentValue
                }
                
                def dependency = new PackageDependency(
                        groupId: groupId,
                        artifactId: artifactId,
                        currentValue: version,
                        depType: scope
                )

                // Handle optional dependencies
                if (dep.optional.text() == 'true') {
                    dependency.depType = 'optional'
                }

                if (dependency.groupId && dependency.artifactId && dependency.currentValue) {
                    println "[INFO] Found dependency: ${dependency}"
                    dependencies << dependency
                } else {
                    println "[WARN] Skipping dependency due to missing information: ${groupId}:${artifactId}"
                }
            }
        }

        // Extract plugins from current POM only
        def buildNode = project.build[0]
        if (buildNode) {
            def pluginsNode = buildNode.plugins[0]
            if (pluginsNode) {
                pluginsNode.plugin.each { plugin ->
                    def groupId = resolveProperty(plugin.groupId.text() ?: 'org.apache.maven.plugins', props)
                    def artifactId = resolveProperty(plugin.artifactId.text(), props)
                    def version = plugin.version.text() ? resolveProperty(plugin.version.text(), props) : null
                    
                    // Check if this is a Spring Boot plugin and version is not specified
                    if (groupId == 'org.springframework.boot' && !version) {
                        version = props['project.parent.version']
                        println "[INFO] Using parent version ${version} for Spring Boot plugin: ${artifactId}"
                    }
                    
                    def dependency = new PackageDependency(
                            groupId: groupId,
                            artifactId: artifactId,
                            currentValue: version,
                            depType: 'build'
                    )

                    if (dependency.artifactId && dependency.currentValue) {
                        println "[INFO] Found plugin: ${dependency}"
                        dependencies << dependency
                    }
                }
            }
        }

        return dependencies
    }

    static List<String> extractRepositories(Node project) {
        def repositories = []

        // Add Maven Central by default
        repositories << MAVEN_CENTRAL

        project.repositories.repository.each { repo ->
            def url = repo.url.text()?.trim()
            if (url) {
                println "[INFO] Found repository: ${url}"
                repositories << url
            }
        }

        return repositories.unique()
    }

    static void processAndUpdate(String pomFile) {
        println "\n=== Processing POM file: ${pomFile} ==="

        def content = new File(pomFile).text
        def project = parsePom(content, pomFile)

        if (!project) {
            println "[ERROR] Failed to process POM file: ${pomFile}"
            return
        }

        def properties = extractProperties(project)
        println "\n=== Extracted Properties ==="
        properties.each { k, v -> println "${k} = ${v}" }

        def dependencies = extractDependencies(project)
        println "\n=== Dependencies in Current POM ==="
        dependencies.each { dep -> println dep }

        def repositories = extractRepositories(project)
        println "\n=== Extracted Repositories ==="
        repositories.each { repo -> println repo }
    }

    static String resolveParentFile(String packageFile, String parentPath) {
        def parentFile = 'pom.xml'
        def parentDir = parentPath
        def parentBasename = new File(parentPath).name

        if (parentBasename == 'pom.xml' || parentBasename.endsWith('.pom.xml')) {
            parentFile = parentBasename
            parentDir = new File(parentPath).parent
        }

        def dir = new File(packageFile).parent
        println "[DEBUG] Resolving parent file: ${parentFile} in dir: ${dir}/${parentDir}"
        return new File(dir, "${parentDir}/${parentFile}").canonicalPath
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

    static PackageFile extractPackage(String rawContent, String packageFile, Map config) {
        if (!rawContent) {
            println "[DEBUG] No content for ${packageFile}"
            return null
        }

        def project = parsePom(rawContent, packageFile)
        if (!project) {
            println "[DEBUG] Failed to parse POM for ${packageFile}"
            return null
        }

        def result = new PackageFile(
                datasource: 'maven',
                packageFile: packageFile,
                deps: []
        )

        // Extract dependencies
        result.deps = extractDependencies(project)
        println "[INFO] Extracted ${result.deps.size()} dependencies"

        // Extract properties
        def propsNode = project.properties[0]
        def props = [:]
        if (propsNode) {
            propsNode.children().each { propNode ->
                def key = propNode.name()
                def val = propNode.text()?.trim()
                if (key && val) {
                    // Simplified as Groovy doesn't track exact file positions
                    props[key] = [val: val, fileReplacePosition: -1, packageFile: packageFile]
                    println "[DEBUG] Found property: ${key} = ${val}"
                }
            }
        }
        result.mavenProps = props

        // Extract repositories
        def repositories = project.repositories[0]
        if (repositories) {
            def repoUrls = []
            repositories.repository.each { repo ->
                def repoUrl = repo.url.text()?.trim()
                if (repoUrl) {
                    repoUrls << repoUrl
                    println "[DEBUG] Found repository: ${repoUrl}"
                }
            }
            result.deps.each { dep ->
                if (dep.registryUrls != null) {
                    dep.registryUrls.addAll(repoUrls)
                }
            }
        }

        // Handle parent POM
        if (packageFile && project.parent) {
            def parentPath = project.parent.relativePath.text()?.trim() ?: '../pom.xml'
            result.parent = resolveParentFile(packageFile, parentPath)
            println "[DEBUG] Found parent POM: ${result.parent}"
        }

        // Extract version
        if (project.version) {
            result.packageFileVersion = project.version.text()?.trim()
            println "[DEBUG] Found package version: ${result.packageFileVersion}"
        }

        return result
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

    static PackageDependency applyProps(PackageDependency dep, String packageFile, Map props) {
        def result = new PackageDependency(
                groupId: resolveProperty(dep.groupId, props),
                artifactId: resolveProperty(dep.artifactId, props),
                currentValue: resolveProperty(dep.currentValue, props),
                depType: dep.depType,
                registryUrls: dep.registryUrls,
                skipReason: dep.skipReason,
                props: dep.props
        )
        result.propSource = props
        return result
    }

    static List<PackageFile> resolveParents(List<PackageFile> packages) {
        def packageFileNames = []
        def extractedPackages = [:]
        def extractedDeps = [:]
        def extractedProps = [:]
        def registryUrls = [:]

        // Initialize data structures
        packages.each { pkg ->
            def name = pkg.packageFile
            packageFileNames << name
            extractedPackages[name] = pkg
            extractedDeps[name] = []
            println "[DEBUG] Processing package: $name}"
        }

        // Process each package
        packageFileNames.each { name ->
            registryUrls[name] = [] as Set
            def propsHierarchy = []
            def visitedPackages = [] as Set
            def pkg = extractedPackages[name]

            // Build properties hierarchy
            while (pkg) {
                // Add current package properties
                propsHierarchy.add(0, (Map) (pkg.mavenProps ?: [:]))

                // Collect registry URLs
                pkg.deps?.each { dep ->
                    dep.registryUrls?.each { url ->
                        registryUrls[name] << url
                    }
                }

                // Process parent if exists and not visited
                if (pkg.parent && !visitedPackages.contains(pkg.parent)) {
                    visitedPackages << pkg.parent
                    
                    // Load parent package if not already loaded
                    if (!extractedPackages.containsKey(pkg.parent)) {
                        try {
                            def parentFile = new File(pkg.parent)
                            if (parentFile.exists()) {
                                def parentContent = parentFile.text
                                def parentPkg = extractPackage(parentContent, pkg.parent, null)
                                if (parentPkg) {
                                    extractedPackages[pkg.parent] = parentPkg
                                    packageFileNames << pkg.parent
                                }
                            } else {
                                println "[ERROR] Parent POM file not found: ${pkg.parent}"
                            }
                        } catch (Exception e) {
                            println "[ERROR] Failed to load parent POM: ${e.message}"
                        }
                    }
                    
                    pkg = extractedPackages[pkg.parent]
                } else {
                    pkg = null
                }
            }

            // Merge properties with parent chain
            extractedProps[name] = propsHierarchy.inject([:]) { acc, val -> 
                // Add reference to parent properties for nested resolution
                val._parentProps = acc
                // Merge properties, child properties override parent
                (Map) (acc + val)
            }
        }

        // Resolve registry URLs
        packageFileNames.each { name ->
            def pkg = extractedPackages[name]
            pkg.deps.each { rawDep ->
                rawDep.registryUrls = ([rawDep.registryUrls, registryUrls[name]].flatten()).unique()
            }
        }

        // Process dependencies and build final result
        def rootDeps = [] as Set
        packageFileNames.each { name ->
            def pkg = extractedPackages[name]
            pkg.deps.each { rawDep ->
                def dep = applyProps(rawDep, name, extractedProps[name])
                if (dep.depType == 'parent') {
                    def parentPkg = extractedPackages[pkg.parent]
                    if ((parentPkg && !parentPkg.parent) ||
                            (parentPkg && !packageFileNames.contains(parentPkg.parent))) {
                        rootDeps << dep.depName
                    }
                }
                def sourceName = dep.propSource ?: name
                extractedDeps[sourceName] << dep
            }
        }

        // Build final package files
        return packageFileNames.collect { packageFile ->
            def pkg = extractedPackages[packageFile]
            def deps = extractedDeps[packageFile]
            deps.each { dep ->
                if (rootDeps.contains(dep.depName)) {
                    dep.depType = 'parent-root'
                }
            }
            return new PackageFile(
                    datasource: pkg.datasource,
                    packageFile: packageFile,
                    deps: deps,
                    packageFileVersion: pkg.packageFileVersion
            )
        }
    }

    static List<PackageFile> cleanResult(List<PackageFile> packageFiles) {
        packageFiles.each { packageFile ->
            packageFile.mavenProps = null
            packageFile.parent = null
            packageFile.deps.each { dep ->
                dep.propSource = null
                if (dep.datasource == 'maven') {
                    dep.registryUrls << MAVEN_REPO
                }
            }
        }
        return packageFiles
    }

    static PackageFile extractExtensions(String rawContent, String packageFile) {
        if (!rawContent) {
            return null
        }

        def extensions = parseExtensions(rawContent, packageFile)
        if (!extensions) {
            return null
        }

        def result = new PackageFile(
                datasource: 'maven',
                packageFile: packageFile,
                deps: []
        )

        result.deps = extractDependencies(extensions)
        return result
    }

    static List<PackageFile> extractAllPackageFiles(Map config, List<String> packageFiles) {
        def packages = []
        def additionalRegistryUrls = []

        packageFiles.each { packageFile ->
            def content = new File(packageFile).text
            if (!content) {
                println "[DEBUG] ${packageFile} has no content"
                return
            }

            if (packageFile.endsWith('settings.xml')) {
                def registries = extractRegistries(content)
                if (registries) {
                    println "[DEBUG] Found registryUrls in settings.xml: ${registries}"
                    additionalRegistryUrls.addAll(registries)
                }
            } else if (packageFile.endsWith('.mvn/extensions.xml')) {
                def extensions = extractExtensions(content, packageFile)
                if (extensions) {
                    packages << extensions
                } else {
                    println "[TRACE] Cannot read extensions in ${packageFile}"
                }
            } else {
                def pkg = extractPackage(content, packageFile, config)
                if (pkg) {
                    packages << pkg
                } else {
                    println "[TRACE] Cannot read dependencies in ${packageFile}"
                }
            }
        }

        if (additionalRegistryUrls) {
            packages.each { pkgFile ->
                pkgFile.deps.each { dep ->
                    dep.registryUrls.addAll(0, additionalRegistryUrls)
                }
            }
        }

        // Process parents to resolve inheritance
        def result = cleanResult(resolveParents(packages))
        
        // After resolving parents, print debug info
        result.each { pkgFile ->
            println "[DEBUG] Final package: ${pkgFile.packageFile}"
            pkgFile.deps.each { dep ->
                println "[DEBUG] Final dependency: ${dep}"
            }
        }
        
        return result
    }

    static String resolveProperty(String value, Map props) {
        if (!value || !(value instanceof String)) {
            return value
        }

        // Handle simple property references ${property}
        if (value.startsWith('${') && value.endsWith('}')) {
            def propKey = value[2..-2]  // Remove ${ and }
            
            // Try to resolve the property
            def propValue = props?.get(propKey)
            if (propValue != null) {
                return propValue
            }
            
            // Try to resolve using project.version if property not found
            if (propKey == 'project.version') {
                return props?.get('project.version')
            }
            
            // Try to resolve using project.parent.version if property not found
            if (propKey == 'project.parent.version') {
                return props?.get('project.parent.version')
            }

            // Try to resolve using parent properties
            if (propKey.startsWith('project.parent.')) {
                def parentPropKey = propKey.substring('project.parent.'.length())
                return props?.get(parentPropKey)
            }
            
            println "[WARN] Could not resolve property: ${propKey}"
            return value  // Return original value if not resolved
        }
        
        // Handle complex values with embedded properties
        def matcher = value =~ /\$\{([^}]*)\}/
        def newValue = value
        while (matcher.find()) {
            def propKey = matcher.group(1)
            def propValue = props?.get(propKey)
            
            if (propValue != null) {
                String placeholder = "${" + propKey + "}"
                newValue = newValue.replace(placeholder, propValue)
            } else {
                println "[WARN] Could not resolve property in complex value: ${propKey}"
            }
        }
        
        return newValue
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

    try {
        def content = pomFile.text
        def project = MavenDependencyManager.parsePom(content, pomFile.path)
        if (!project) {
            println "[ERROR] Failed to parse POM file: ${pomFile.path}"
            System.exit(1)
        }

        def dependencies = MavenDependencyManager.extractDependencies(project)

        // Group dependencies by type for clear output
        def grouped = dependencies.groupBy { it.depType }

        println "\n=== Detailed Dependencies ==="
        grouped.each { type, deps ->
            println "\n${type.toUpperCase()}:"
            deps.each { dep ->
                println "- ${dep}"
            }
        }

    } catch (Exception e) {
        println "[ERROR] Failed to process POM: ${e.message}"
        e.printStackTrace()
        System.exit(1)
    }
}