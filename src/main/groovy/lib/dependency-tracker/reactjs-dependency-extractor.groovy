#!/usr/bin/env groovy
import groovy.json.JsonException
import groovy.json.JsonSlurper

class ReactDependencyExtractor {
    static final String NPM_REGISTRY = "https://registry.npmjs.org/"
    static final String YARN_REGISTRY = "https://registry.yarnpkg.com/"
    static final String PNPM_REGISTRY = "https://registry.npmjs.org/"

    static class Dependency {
        String name
        String version
        String type // e.g., "dependencies", "devDependencies", etc.
        List<String> registryUrls = []
        String skipReason
        Map<String, Object> props = [:]
        boolean isManaged = false

        String toString() {
            return "${name}@${version} (${type})"
        }
    }

    static class PackageFile {
        String datasource
        String packageFile
        List<Dependency> deps = []
        Map<String, Object> npmProps
        String parent
        String packageFileVersion
    }

    static String resolveVersion(String version, Map packageJson) {
        if (!version) return "UNKNOWN"

        // Handle semver ranges
        if (version.startsWith('^') || version.startsWith('~')) {
            return version
        }

        // Handle workspace references
        if (version.startsWith('workspace:')) {
            return version.substring('workspace:'.length())
        }

        // Handle file: references
        if (version.startsWith('file:')) {
            return version.substring('file:'.length())
        }

        // Handle git references
        if (version.startsWith('git+')) {
            return version
        }

        return version
    }

    static String detectFramework(File packageJsonFile) {
        def packageJson = new JsonSlurper().parse(packageJsonFile)

        if (packageJson.dependencies?.react) {
            return "REACT"
        } else if (packageJson.dependencies?.'@angular/core') {
            return "ANGULAR"
        } else if (packageJson.dependencies?.vue) {
            return "VUE"
        }
        return "JAVASCRIPT"
    }

    static String determineBuildManager(String projectDir) {
        def files = new File(projectDir).listFiles()

        // Check for build tool configuration files
        if (files.any { it.name == 'vite.config.js' || it.name == 'vite.config.ts' }) {
            return 'VITE'
        } else if (files.any { it.name == 'next.config.js' || it.name == 'next.config.ts' }) {
            return 'NEXT'
        } else if (files.any { it.name == 'craco.config.js' || it.name == 'craco.config.ts' }) {
            return 'CRA'
        } else if (files.any { it.name == 'yarn.lock' }) {
            return 'YARN'
        } else if (files.any { it.name == 'pnpm-lock.yaml' }) {
            return 'PNPM'
        } else if (files.any { it.name == 'package-lock.json' }) {
            return 'NPM'
        }
        return 'NPM'
    }

    static String detectRuntime(File packageJsonFile) {
        def packageJson = new JsonSlurper().parse(packageJsonFile)
        def runtime = []

        // Check engines field
        if (packageJson.engines?.node) {
            runtime << "Node.js ${packageJson.engines.node}"
        }

        // Check for .nvmrc
        def nvmrcFile = new File(packageJsonFile.parent, ".nvmrc")
        if (nvmrcFile.exists()) {
            runtime << "Node.js ${nvmrcFile.text.trim()}"
        }

        // Check for .node-version
        def nodeVersionFile = new File(packageJsonFile.parent, ".node-version")
        if (nodeVersionFile.exists()) {
            runtime << "Node.js ${nodeVersionFile.text.trim()}"
        }

        return runtime.join(', ') ?: "Node.js"
    }

    static List<Dependency> extractDependencies(Map packageJson) {
        def dependencies = []

        // Extract from all possible dependency sections
        ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies', 'resolutions'].each { section ->
            if (packageJson[section]) {
                packageJson[section].each { name, version ->
                    def dependency = new Dependency(
                            name: name,
                            version: resolveVersion(version, packageJson),
                            type: section,
                            registryUrls: [NPM_REGISTRY, YARN_REGISTRY, PNPM_REGISTRY]
                    )

                    // Add additional metadata
                    if (packageJson.engines?[name]) {
                        dependency.props['engine'] = packageJson.engines[name]
                    }

                    dependencies << dependency
                }
            }
        }

        return dependencies
    }

    static List<PackageFile> extractWorkspacePackages(File rootDir) {
        def workspaces = []
        def packageJson = new JsonSlurper().parse(new File(rootDir, "package.json"))

        // Handle different workspace formats
        def workspacePatterns = []
        if (packageJson.workspaces) {
            if (packageJson.workspaces instanceof List) {
                workspacePatterns = packageJson.workspaces
            } else if (packageJson.workspaces.packages) {
                workspacePatterns = packageJson.workspaces.packages
            }
        }

        workspacePatterns.each { pattern ->
            def workspaceDir = new File(rootDir, pattern)
            if (workspaceDir.exists()) {
                workspaceDir.eachDirRecurse { dir ->
                    def packageFile = new File(dir, "package.json")
                    if (packageFile.exists()) {
                        workspaces << extractDetails(packageFile)
                    }
                }
            }
        }
        return workspaces
    }

    static Map extractDetails(File packageJsonFile) {
        println "\n=== Analyzing package.json: ${packageJsonFile.absolutePath} ==="

        if (!packageJsonFile.exists()) {
            println "[ERROR] package.json file doesn't exist"
            return [componentId: null, dependencies: [], runtimeVersion: null, compilerVersion: null]
        }

        def jsonSlurper = new JsonSlurper()
        def packageJson

        try {
            packageJson = jsonSlurper.parse(packageJsonFile)
        } catch (JsonException e) {
            println "[ERROR] Failed to parse package.json: ${e.message}"
            return [componentId: null, dependencies: [], runtimeVersion: null, compilerVersion: null]
        }

        def result = [
                componentId    : packageJson.name,
                dependencies   : extractDependencies(packageJson),
                runtimeVersion : detectRuntime(packageJsonFile),
                buildManager   : determineBuildManager(packageJsonFile.parent),
                framework      : detectFramework(packageJsonFile)
        ]

        // Extract workspace dependencies if any
        try {
            def workspaces = extractWorkspacePackages(packageJsonFile.parentFile)
            if (workspaces) {
                workspaces.each { workspace ->
                    if (workspace.dependencies) {
                        result.dependencies.addAll(workspace.dependencies)
                    }
                }
            }
        } catch (Exception e) {
            println "[WARN] Failed to extract workspace packages: ${e.message}"
            // Continue without workspace dependencies
        }

        // Print summary
        println "\n=== Summary ==="
        println "Component ID: ${result.componentId ?: 'N/A'}"
        println "Runtime Version: ${result.runtimeVersion ?: 'N/A'}"
        println "Build Manager: ${result.buildManager}"
        println "Framework: ${result.framework}"
        println "Total dependencies: ${result.dependencies.size()}"
        result.dependencies.groupBy { it.type }.each { type, deps ->
            println "- ${type}: ${deps.size()}"
        }

        return result
    }

    static void uploadDependency(String compiler, String runtimeVersion, String framework, String buildManager, String componentId, String branch, String sourceCodeUrl, List<Dependency> dependencies) {
        println "::debug::Pushing dependencies to Dependency Tracker API"
        def apiUrl = System.getenv('DEPENDENCY_TRACKER_API_URL')
        if (!apiUrl) {
            println "::warn::DEPENDENCY_TRACKER_API_URL environment variable is not set, will try the default url."
            apiUrl = "http://localhost:8080/dependency-tracker/api/v1/components"
        }

        def apiToken = System.getenv('DEPENDENCY_TRACKER_API_TOKEN')
        if (!apiToken) {
            println "::warn::DEPENDENCY_TRACKER_API_TOKEN environment variable is not set"
            apiToken = ""
        }

        def name = extractNameFromSourceCodeUrl(sourceCodeUrl)

        def payload = [
                component     : [
                        name         : name,
                        sourceCodeUrl: sourceCodeUrl,
                        eimId        : '',
                ],
                componentId   : componentId,
                branch        : branch,
                compiler      : compiler,
                runtimeVersion: runtimeVersion,
                language      : framework,
                buildManager  : buildManager,
                dependencies  : dependencies.collect { dep ->
                    [
                            artefact: dep.name,
                            version : dep.version,
                            type    : dep.type
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

                connection.outputStream.withWriter { writer ->
                    writer << new groovy.json.JsonBuilder(payload).toString()
                }

                def responseCode = connection.responseCode
                def responseBody = connection.inputStream.text

                if (responseCode == 200) {
                    println "::notice::Successfully pushed dependencies for ${componentId}"
                    if (System.getenv('CI')) {
                        println "::set-output name=status::success"
                        println "::set-output name=componentId::${componentId}"
                    }
                    success = true
                } else if (responseCode == 429) {
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
                    success = true
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
                    retryCount++
                }
            }
        }
    }

    static String extractNameFromSourceCodeUrl(String sourceCodeUrl) {
        try {
            if (sourceCodeUrl.contains('github.com')) {
                return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
            } else if (sourceCodeUrl.contains('gitlab.com')) {
                return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
            } else if (sourceCodeUrl.contains('bitbucket.org')) {
                return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
            }
            return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
        } catch (Exception e) {
            println "[WARN] Failed to extract name from sourceCodeUrl: ${sourceCodeUrl}"
            return 'unknown-component'
        }
    }

    // Main method for testing
    static void main(String[] args) {
        if (args.length == 0) {
            println """
                Usage: groovy ReactDependencyExtractor.groovy <path to package.json> [branch] [sourceCodeUrl] [compiler]
                Example: groovy ReactDependencyExtractor.groovy ./package.json main https://github.com/org/repo typescript
                """
            System.exit(1)
        }

        def packageJsonFile = new File(args[0])
        def branch = args.length > 1 ? args[1] : "main"
        def sourceCodeUrl = args.length > 2 ? args[2] : ""
        def compiler = args.length > 3 ? args[3] : "nodejs"

        try {
            def result = extractDetails(packageJsonFile)
            def componentId = result.componentId
            def dependencies = result.dependencies
            def runtimeVersion = result.runtimeVersion
            def framework = result.framework
            def buildManager = result.buildManager

            if (componentId && sourceCodeUrl) {
                uploadDependency(compiler, runtimeVersion, framework, buildManager, componentId, branch, sourceCodeUrl, dependencies)
            }

            // Group dependencies by type for clear output
            def grouped = dependencies.groupBy { it.type }

            println "\n=== Detailed Dependencies ==="
            grouped.each { type, deps ->
                println "\n${type.toUpperCase()}:"
                deps.each { dep ->
                    println "- ${dep}"
                }
            }

            println "\n=== Component ID ==="
            println componentId ?: "No componentId found"

            println "\n=== Runtime Version ==="
            println runtimeVersion ?: "No runtime version found"

            println "\n=== Compiler Version ==="
            println compiler ?: "No compiler found"

        } catch (Exception e) {
            println "[ERROR] Failed to process package.json: ${e.message}"
            e.printStackTrace()
            System.exit(1)
        }
    }
}