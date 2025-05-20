#!/usr/bin/env groovy
import groovy.json.JsonSlurper
import java.io.StringReader
import java.util.regex.Pattern

class PythonDependencyExtractor {
    static final String PYPI_REGISTRY = "https://pypi.org/simple/"
    static final String CONDA_REGISTRY = "https://repo.anaconda.com/pkgs/main/"
    static final String ARTIFACTORY_REGISTRY = "https://artifactory.example.com/api/pypi/pypi/simple/"

    static class Dependency {
        String name
        String version
        String type // e.g., "dependencies", "devDependencies", "testDependencies"
        List<String> registryUrls = []
        String skipReason
        Map<String, Object> props = [:]
        boolean isManaged = false
        String source // e.g., "requirements.txt", "pyproject.toml", etc.

        String toString() {
            return "${name}==${version} (${type} from ${source})"
        }
    }

    static class PackageFile {
        String datasource
        String packageFile
        List<Dependency> deps = []
        Map<String, Object> pythonProps
        String parent
        String packageFileVersion
    }

    static String extractComponentId(File projectDir, String sourceCodeUrl) {
        // Try to get from git sourceCodeUrl first
        if (sourceCodeUrl) {
            try {
                if (sourceCodeUrl.contains('github.com')) {
                    return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
                } else if (sourceCodeUrl.contains('gitlab.com')) {
                    return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
                } else if (sourceCodeUrl.contains('bitbucket.org')) {
                    return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from sourceCodeUrl: ${e.message}"
            }
        }

        // Try to get from setup.py
        def setupFile = new File(projectDir, "setup.py")
        if (setupFile.exists()) {
            try {
                def content = setupFile.text
                def nameMatch = content =~ /name\s*=\s*['"]([^'"]+)['"]/
                if (nameMatch.find()) {
                    return nameMatch.group(1)
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from setup.py: ${e.message}"
            }
        }

        // Try to get from setup.cfg
        def setupCfgFile = new File(projectDir, "setup.cfg")
        if (setupCfgFile.exists()) {
            try {
                def content = setupCfgFile.text
                def nameMatch = content =~ /name\s*=\s*([^\n]+)/
                if (nameMatch.find()) {
                    return nameMatch.group(1).trim()
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from setup.cfg: ${e.message}"
            }
        }

        // Try to get from pyproject.toml
//        def pyprojectFile = new File(projectDir, "pyproject.toml")
//        if (pyprojectFile.exists()) {
//            try {
//                def reader = new StringReader(pyprojectFile.text)
//                def toml = new toml.Toml().read(reader)
//                def toolPoetry = toml.getTable("tool.poetry")
//                if (toolPoetry != null && toolPoetry.get("name") != null) {
//                    return toolPoetry.getString("name")
//                }
//            } catch (Exception e) {
//                println "[WARN] Failed to extract componentId from pyproject.toml: ${e.message}"
//            }
//        }

        // Fallback to parent directory name
        return projectDir.parentFile.name
    }

    static String detectRuntime(File projectDir) {
        def runtime = []

        // Check .python-version
        def pythonVersionFile = new File(projectDir, ".python-version")
        if (pythonVersionFile.exists()) {
            runtime << "Python ${pythonVersionFile.text.trim()}"
        }

        // Check runtime.txt (Heroku)
        def runtimeFile = new File(projectDir, "runtime.txt")
        if (runtimeFile.exists()) {
            runtime << runtimeFile.text.trim()
        }

        // Check Dockerfile
        def dockerfile = new File(projectDir, "Dockerfile")
        if (dockerfile.exists()) {
            def content = dockerfile.text
            def pythonMatch = content =~ /FROM\s+python:([^\s]+)/
            if (pythonMatch.find()) {
                runtime << "Python ${pythonMatch.group(1)}"
            }
        }

        // Check docker-compose.yml
        def dockerComposeFile = new File(projectDir, "docker-compose.yml")
        if (dockerComposeFile.exists()) {
            try {
                def yaml = new JsonSlurper().parseText(dockerComposeFile.text)
                yaml.services.each { service ->
                    if (service.value.image?.contains('python')) {
                        runtime << "Python ${service.value.image.split(':')[1]}"
                    }
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse docker-compose.yml: ${e.message}"
            }
        }

        // Check GitHub Actions workflows
        def workflowsDir = new File(projectDir, ".github/workflows")
        if (workflowsDir.exists()) {
            workflowsDir.eachFile { file ->
                if (file.name.endsWith('.yml') || file.name.endsWith('.yaml')) {
                    try {
                        def content = file.text
                        def pythonMatch = content =~ /python-version:\s*['"]?([^'"]+)['"]?/
                        if (pythonMatch.find()) {
                            runtime << "Python ${pythonMatch.group(1)}"
                        }
                    } catch (Exception e) {
                        println "[WARN] Failed to parse workflow file ${file.name}: ${e.message}"
                    }
                }
            }
        }

        return runtime.unique().join(', ') ?: "UNKNOWN"
    }

    static String detectBuildManager(File projectDir) {
        def files = projectDir.listFiles()

        if (files.any { it.name == 'Pipfile' || it.name == 'Pipfile.lock' }) {
            return 'PIPENV'
        } else if (files.any { it.name == 'pyproject.toml' }) {
            return 'POETRY'
        } else if (files.any { it.name == 'environment.yml' || it.name == 'conda.yml' }) {
            return 'CONDA'
        } else if (files.any { it.name == 'setup.py' || it.name == 'setup.cfg' }) {
            return 'SETUPTOOLS'
        }
        return 'PIP'
    }

    static List<Dependency> extractFromRequirements(File requirementsFile) {
        println "\n=== Analyzing requirements.txt: ${requirementsFile.absolutePath} ==="

        if (!requirementsFile.exists()) {
            println "[ERROR] requirements.txt file doesn't exist"
            return []
        }

        def dependencies = []
        requirementsFile.eachLine { line ->
            line = line.trim()
            if (line && !line.startsWith("#")) {
                // Handle different requirement formats
                def name, version, type = "dependencies"

                // Handle -r or --requirement
                if (line.startsWith("-r ") || line.startsWith("--requirement ")) {
                    def subFile = new File(requirementsFile.parent, line.split(" ")[1])
                    if (subFile.exists()) {
                        dependencies.addAll(extractFromRequirements(subFile))
                    }
                    return
                }

                // Handle -e or --editable
                if (line.startsWith("-e ") || line.startsWith("--editable ")) {
                    name = line.split(" ")[1]
                    version = "editable"
                    type = "editable"
                } else {
                    // Handle standard requirements
                    def parts = line.split("==")
                    if (parts.length == 1) {
                        parts = line.split(">=")
                        if (parts.length == 1) {
                            parts = line.split("<=")
                            if (parts.length == 1) {
                                parts = line.split("~=")
                            }
                        }
                    }
                    name = parts[0].trim()
                    version = parts.length > 1 ? parts[1].trim() : "unspecified"
                }

                def dependency = new Dependency(
                        name: name,
                        version: version,
                        type: type,
                        source: "requirements.txt",
                        registryUrls: [PYPI_REGISTRY]
                )
                dependencies << dependency
                println "[INFO] Found dependency: ${dependency}"
            }
        }

        return dependencies
    }


//    static List<Dependency> extractFromPipfile(File pipfile) {
//        println "\n=== Analyzing Pipfile: ${pipfile.absolutePath} ==="
//
//        if (!pipfile.exists()) {
//            println "[ERROR] Pipfile doesn't exist"
//            return []
//        }
//
//        def dependencies = []
//        try {
//            def reader = new StringReader(pipfile.text)
//            def toml = new toml.Toml().read(reader)
//            // Extract from different sections
//            def sections = [
//                    "packages"    : "dependencies",
//                    "dev-packages": "devDependencies"
//            ]
//
//            sections.each { section, type ->
//                if (toml.contains(section)) {
//                    def deps = toml.getTable(section)
//                    deps?.keySet()?.each { key ->
//                        def value = deps.get(key)
//                        def version = value instanceof String ? value : "unspecified"
//                        dependencies << new Dependency(
//                                name: key,
//                                version: version,
//                                type: type,
//                                source: "Pipfile",
//                                registryUrls: [PYPI_REGISTRY]
//                        )
//                        println "[INFO] Found ${type} dependency: ${key}==${version}"
//                    }
//                }
//            }
//        } catch (Exception e) {
//            println "[ERROR] Failed to parse Pipfile: ${e.message}"
//        }
//
//        return dependencies
//    }

    static List<Dependency> extractFromSetupPy(File setupFile) {
        println "\n=== Analyzing setup.py: ${setupFile.absolutePath} ==="

        if (!setupFile.exists()) {
            println "[ERROR] setup.py doesn't exist"
            return []
        }

        def dependencies = []
        try {
            def content = setupFile.text

            // Extract install_requires
            def installRequiresMatch = (content =~ /install_requires\s*=\s*$$(.*?)$$/)

            if (installRequiresMatch.find()) {
                def deps = installRequiresMatch.group(1).split(',').collect { it.trim().replaceAll(/['"]/, '') }
                deps.each { dep ->
                    def parts = dep.split("==")
                    def name = parts[0].trim()
                    def version = parts.length > 1 ? parts[1].trim() : "unspecified"
                    def dependency = new Dependency(
                            name: name,
                            version: version,
                            type: "dependencies",
                            source: "setup.py",
                            registryUrls: [PYPI_REGISTRY]
                    )
                    dependencies << dependency
                    println "[INFO] Found dependency: ${dependency}"
                }
            }

            // Extract extras_require
            def extrasRequireMatch = (content =~ /extras_require\s*=\s*\{([^}]*)\}/)
            if (extrasRequireMatch.find()) {
                def extras = extrasRequireMatch.group(1)
                extras.split(',').each { extra ->
                    def parts = extra.split(':')
                    if (parts.length > 1) {
                        def type = parts[0].trim().replaceAll(/['"]/, '')
                        def deps = parts[1].trim().replaceAll(/[\[\]'"]/, '').split(',').collect { it.trim() }
                        deps.each { dep ->
                            def depParts = dep.split("==")
                            def name = depParts[0].trim()
                            def version = depParts.length > 1 ? depParts[1].trim() : "unspecified"
                            def dependency = new Dependency(
                                    name: name,
                                    version: version,
                                    type: "extras.${type}",
                                    source: "setup.py",
                                    registryUrls: [PYPI_REGISTRY]
                            )
                            dependencies << dependency
                            println "[INFO] Found extra dependency: ${dependency}"
                        }
                    }
                }
            }
        } catch (Exception e) {
            println "[ERROR] Failed to parse setup.py: ${e.message}"
        }

        return dependencies
    }

    static List<Dependency> extractFromPyproject(File pyprojectFile) {
        println "\n=== Analyzing pyproject.toml: ${pyprojectFile.absolutePath} ==="

        if (!pyprojectFile.exists()) {
            println "[ERROR] pyproject.toml file doesn't exist"
            return []
        }

        def dependencies = []
        try {
//            def tomlContent = pyprojectFile.text
//            def reader = new StringReader(tomlContent)
//            def toml = new toml.Toml().read(reader)

            // Extract dependencies from different sections
            def sections = [
                "tool.poetry.dependencies"           : "dependencies",
                "tool.poetry.dev-dependencies"       : "devDependencies",
                "tool.poetry.group.dev.dependencies" : "devDependencies",
                "tool.poetry.group.test.dependencies": "testDependencies"
            ]

//            sections.each { section, type ->
//                if (toml.contains(section)) {
//                    def deps = toml.getTable(section)
//                    deps.keySet().each { key ->
//                        def value = deps.get(key)
//                        def version = value instanceof String ? value : "unspecified"
//                        def dependency = new Dependency(
//                            name: key,
//                            version: version,
//                            type: type,
//                            source: "pyproject.toml",
//                            registryUrls: [PYPI_REGISTRY]
//                        )
//                        dependencies << dependency
//                        println "[INFO] Found ${type} dependency: ${dependency}"
//                    }
//                }
//            }
        } catch (Exception e) {
            println "[ERROR] Failed to parse pyproject.toml: ${e.message}"
        }

        return dependencies
    }

    static Map<String, String> detectVirtualEnvironments(File projectDir) {
        def venvs = [:]

        // Check for common virtual environment directories
        def venvDirs = [
                'venv', '.venv', 'env', '.env',
                'virtualenv', '.virtualenv'
        ]

        venvDirs.each { dir ->
            def venvDir = new File(projectDir, dir)
            if (venvDir.exists() && venvDir.isDirectory()) {
                def pythonExe = new File(venvDir, "bin/python")
                if (!pythonExe.exists()) {
                    pythonExe = new File(venvDir, "Scripts/python.exe")
                }
                if (pythonExe.exists()) {
                    venvs[dir] = pythonExe.absolutePath
                }
            }
        }

        return venvs
    }

    static List<String> detectPackageManagers(File projectDir) {
        def packageManagers = []

        // Check for pip
        def pipFiles = [
                'pip.conf', '.pip/pip.conf',
                'pip.ini', '.pip/pip.ini'
        ]

        pipFiles.each { file ->
            if (new File(projectDir, file).exists()) {
                packageManagers << 'PIP'
                return
            }
        }

        // Check for Pipfile (removed TOML-related code)
        if (new File(projectDir, "Pipfile").exists()) {
            packageManagers << "pip"
        }

        // Check for poetry
        if (new File(projectDir, 'pyproject.toml').exists()) {
            packageManagers << 'POETRY'
        }

        // Check for conda
        if (new File(projectDir, 'environment.yml').exists() ||
                new File(projectDir, 'conda.yml').exists()) {
            packageManagers << 'CONDA'
        }

        return packageManagers
    }

    static List<String> detectPackageRepositories(File projectDir) {
        def repos = []

        // Check pip.conf
        def pipConf = new File(projectDir, 'pip.conf')
        if (pipConf.exists()) {
            def content = pipConf.text
            def indexMatch = content =~ /index-url\s*=\s*(.+)$/
            if (indexMatch.find()) {
                repos << indexMatch.group(1).trim()
            }
        }

        // Check .pypirc
        def pypirc = new File(projectDir, '.pypirc')
        if (pypirc.exists()) {
            def content = pypirc.text
            def repoMatch = content =~ /repository\s*=\s*(.+)$/
            if (repoMatch.find()) {
                repos << repoMatch.group(1).trim()
            }
        }

        // TOML-related repository detection
//        def pyproject = new File(projectDir, 'pyproject.toml')
//        if (pyproject.exists()) {
//            try {
//                def tomlContent = pyproject.text
//                def reader = new StringReader(tomlContent)
//                def toml = new toml.Toml().read(reader)
//                if (toml.contains('tool.poetry.source')) {
//                    def sources = toml.getArray('tool.poetry.source')
//                    sources.each { source ->
//                        if (source.url) {
//                            repos << source.url
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                println "[WARN] Failed to parse pyproject.toml: ${e.message}"
//            }
//        }

        return repos.unique()
    }


    static void uploadDependency(String compiler, String runtimeVersion, String componentId, String branch, String sourceCodeUrl, List<Dependency> dependencies) {
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
                runtimeVersion: runtimeVersion,
                language: 'PYTHON',
                buildManager: detectBuildManager(new File("."))
        ]

        // Convert dependencies for the payload
        payload.dependencies = dependencies.collect { dep ->
            [
                    artefact: "${dep.name}",
                    version : dep.version,
                    type: "runtime",
            ]
        }

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
                    retryCount++
                }
            }
        }
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

    static void main(String[] args) {
        if (args.length == 0) {
            println """
            Usage: groovy PythonDependencyExtractor.groovy <project_directory>
            Example: groovy PythonDependencyExtractor.groovy . 
            """
            System.exit(1)
        }

        def projectDir = new File(args[0])
        if (!projectDir.exists()) {
            println "[ERROR] Project directory or file does not exist: ${projectDir}"
            System.exit(1)
        }

        if(!projectDir.isDirectory()){
            projectDir = projectDir.getParentFile()
        }

        def branch = args.size() > 1 ? args[1] : "main"
        def sourceCodeUrl = args.size() > 2 ? args[2] : null
        def compiler = args.size() > 3 ? args[3] : "CPython"

        try {
            // Detect component ID
            def componentId = extractComponentId(projectDir, sourceCodeUrl)

            // Detect runtime version
            def runtimeVersion = detectRuntime(projectDir)

            // Extract dependencies from various sources
            def dependencies = []

            // Check for requirements.txt
            def requirementsFile = new File(projectDir, "requirements.txt")
            if (requirementsFile.exists()) {
                dependencies.addAll(extractFromRequirements(requirementsFile))
            }

            // Check for Pipfile
//            def pipfile = new File(projectDir, "Pipfile")
//            if (pipfile.exists()) {
//                dependencies.addAll(extractFromPipfile(pipfile))
//            }

            // Check for setup.py
            def setupFile = new File(projectDir, "setup.py")
            if (setupFile.exists()) {
                dependencies.addAll(extractFromSetupPy(setupFile))
            }

            // Upload dependencies
            uploadDependency(compiler, runtimeVersion, componentId, branch, sourceCodeUrl, dependencies)

            // Group dependencies by type for clear output
            def grouped = dependencies.groupBy { it.type }

            println "\n=== Detailed Dependencies ==="
            grouped.each { type, deps ->
                println "\n${type.toUpperCase()}:"
                deps.each { dep -> println "- ${dep}" }
            }
        } catch (Exception e) {
            println "[ERROR] Failed to process project: ${e.message}"
            e.printStackTrace()
            System.exit(1)
        }
    }
}