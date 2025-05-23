#!/usr/bin/env groovy
import groovy.json.JsonBuilder

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
                if (sourceCodeUrl.contains('github.com') || sourceCodeUrl.contains('gitlab.com') || sourceCodeUrl.contains('bitbucket.org')) {
                    def name = sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replace('.git', '')
                    if (name && name != '') return name
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from sourceCodeUrl: ${e.message}"
            }
        }

        // Try to get from pyproject.toml first (most modern)
        def pyprojectFile = new File(projectDir, "pyproject.toml")
        if (pyprojectFile.exists()) {
            try {
                def content = pyprojectFile.text
                // Handle different pyproject.toml formats
                def patterns = [
                        /\[project\]\s*\n[^[]*name\s*=\s*['"]([^'"]+)['"]/,
                        /\[tool\.poetry\]\s*\n[^[]*name\s*=\s*['"]([^'"]+)['"]/,
                        /name\s*=\s*['"]([^'"]+)['"]/
                ]

                for (def pattern : patterns) {
                    def matcher = content =~ pattern
                    if (matcher.find()) {
                        return matcher.group(1)
                    }
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from pyproject.toml: ${e.message}"
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
                def nameMatch = content =~ /name\s*=\s*([^\n\r]+)/
                if (nameMatch.find()) {
                    return nameMatch.group(1).trim()
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from setup.cfg: ${e.message}"
            }
        }

        // Try to get from Pipfile
        def pipfile = new File(projectDir, "Pipfile")
        if (pipfile.exists()) {
            try {
                def content = pipfile.text
                // Look for [metadata] section
                def nameMatch = content =~ /\[metadata\][^[]*name\s*=\s*['"]([^'"]+)['"]/
                if (nameMatch.find()) {
                    return nameMatch.group(1)
                }
            } catch (Exception e) {
                println "[WARN] Failed to extract componentId from Pipfile: ${e.message}"
            }
        }

        // Try to get from __init__.py or _version.py
        def initFiles = ["__init__.py", "_version.py", "version.py"]
        for (def fileName : initFiles) {
            def file = new File(projectDir, fileName)
            if (file.exists()) {
                try {
                    def content = file.text
                    def nameMatch = content =~ /__package__\s*=\s*['"]([^'"]+)['"]/
                    if (nameMatch.find()) {
                        return nameMatch.group(1)
                    }
                } catch (Exception e) {
                    println "[WARN] Failed to extract componentId from ${fileName}: ${e.message}"
                }
            }
        }

        // Fallback to directory name
        return projectDir.name
    }

    static String detectRuntime(File projectDir) {
        // Check .python-version (pyenv) - highest priority
        def pythonVersionFile = new File(projectDir, ".python-version")
        if (pythonVersionFile.exists()) {
            def version = pythonVersionFile.text.trim()
            return "${version}"
        }

        // Check runtime.txt (Heroku)
        def runtimeFile = new File(projectDir, "runtime.txt")
        if (runtimeFile.exists()) {
            def runtime = runtimeFile.text.trim()
            if (runtime.startsWith("python-")) {
                return "${runtime.replace('python-', '')}"
            } else {
                return "${runtime}"
            }
        }

        // Check pyproject.toml for Python version requirements
        def pyprojectFile = new File(projectDir, "pyproject.toml")
        if (pyprojectFile.exists()) {
            try {
                def content = pyprojectFile.text
                // Look for requires-python or python version constraints
                def patterns = [
                        /requires-python\s*=\s*['"]([^'"]+)['"]/,
                        /python\s*=\s*['"]([^'"]+)['"]/
                ]

                for (def pattern : patterns) {
                    def matcher = content =~ pattern
                    if (matcher.find()) {
                        return "${matcher.group(1)}"
                    }
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse pyproject.toml for Python version: ${e.message}"
            }
        }

        // Check Pipfile for Python version
        def pipfile = new File(projectDir, "Pipfile")
        if (pipfile.exists()) {
            try {
                def content = pipfile.text
                def pythonMatch = content =~ /python_version\s*=\s*['"]([^'"]+)['"]/
                if (pythonMatch.find()) {
                    return "${pythonMatch.group(1)}"
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse Pipfile for Python version: ${e.message}"
            }
        }

        // Check setup.py for Python version requirements
        def setupFile = new File(projectDir, "setup.py")
        if (setupFile.exists()) {
            try {
                def content = setupFile.text
                def pythonMatch = content =~ /python_requires\s*=\s*['"]([^'"]+)['"]/
                if (pythonMatch.find()) {
                    return "${pythonMatch.group(1)}"
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse setup.py for Python version: ${e.message}"
            }
        }

        // Check Dockerfile
        def dockerfile = new File(projectDir, "Dockerfile")
        if (dockerfile.exists()) {
            try {
                def content = dockerfile.text
                def pythonMatch = content =~ /FROM\s+python:([^\s\n]+)/
                if (pythonMatch.find()) {
                    return "${pythonMatch.group(1)}"
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse Dockerfile: ${e.message}"
            }
        }

        // Check docker-compose.yml
        def dockerComposeFiles = ["docker-compose.yml", "docker-compose.yaml"]
        for (def fileName : dockerComposeFiles) {
            def dockerComposeFile = new File(projectDir, fileName)
            if (dockerComposeFile.exists()) {
                try {
                    def content = dockerComposeFile.text
                    def pythonMatch = content =~ /image:\s*python:([^\s\n]+)/
                    if (pythonMatch.find()) {
                        return "${pythonMatch.group(1)}"
                    }
                } catch (Exception e) {
                    println "[WARN] Failed to parse ${fileName}: ${e.message}"
                }
            }
        }

        // Check GitHub Actions workflows
        def workflowsDir = new File(projectDir, ".github/workflows")
        if (workflowsDir.exists() && workflowsDir.isDirectory()) {
            def foundVersion = null
            workflowsDir.eachFile { file ->
                if ((file.name.endsWith('.yml') || file.name.endsWith('.yaml')) && !foundVersion) {
                    try {
                        def content = file.text
                        def patterns = [
                                /python-version:\s*['"]?([^'"\n\r]+)['"]?/,
                                /python_version:\s*['"]?([^'"\n\r]+)['"]?/
                        ]

                        for (def pattern : patterns) {
                            def matcher = content =~ pattern
                            if (matcher.find()) {
                                foundVersion = "${matcher.group(1)}"
                                break
                            }
                        }
                    } catch (Exception e) {
                        println "[WARN] Failed to parse workflow file ${file.name}: ${e.message}"
                    }
                }
            }
            if (foundVersion) {
                return foundVersion
            }
        }

        // Check tox.ini
        def toxFile = new File(projectDir, "tox.ini")
        if (toxFile.exists()) {
            try {
                def content = toxFile.text
                def pythonMatch = content =~ /envlist\s*=\s*([^\n\r]+)/
                if (pythonMatch.find()) {
                    def envs = pythonMatch.group(1).split(",")
                    for (def env : envs) {
                        if (env.trim().startsWith("py")) {
                            def version = env.trim().replace("py", "").replace("python", "")
                            if (version.isNumber() && version.length() >= 2) {
                                def major = version.substring(0, 1)
                                def minor = version.substring(1)
                                return "${major}.${minor}"
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse tox.ini: ${e.message}"
            }
        }

        // Default fallback
        return "UNKNOWN"
    }

    static String detectBuildManager(File projectDir) {
        def files = projectDir.listFiles()?.collect { it.name } ?: []

        // Priority order for detection
        if ('pyproject.toml' in files) {
            def content = new File(projectDir, 'pyproject.toml').text
            if (content.contains('[tool.poetry]')) {
                return 'POETRY'
            } else if (content.contains('[build-system]')) {
                if (content.contains('setuptools')) {
                    return 'SETUPTOOLS'
                } else if (content.contains('flit')) {
                    return 'FLIT'
                } else if (content.contains('hatchling')) {
                    return 'HATCH'
                }
                return 'PEP517'
            }
        }

        if ('Pipfile' in files || 'Pipfile.lock' in files) {
            return 'PIPENV'
        }

        if ('environment.yml' in files || 'conda.yml' in files || 'environment.yaml' in files) {
            return 'CONDA'
        }

        if ('setup.py' in files || 'setup.cfg' in files) {
            return 'SETUPTOOLS'
        }

        if ('requirements.txt' in files || 'requirements.in' in files) {
            return 'PIP'
        }

        return 'UNKNOWN'
    }

    static List<Dependency> extractFromRequirements(File requirementsFile, String type = "dependencies") {
        println "\n=== Analyzing requirements file: ${requirementsFile.absolutePath} ==="

        if (!requirementsFile.exists()) {
            println "[ERROR] Requirements file doesn't exist"
            return []
        }

        def dependencies = []
        def lineNumber = 0

        requirementsFile.eachLine { line ->
            lineNumber++
            line = line.trim()

            // Skip empty lines and comments
            if (!line || line.startsWith("#") || line.startsWith("-f") || line.startsWith("--find-links")) {
                return
            }

            try {
                // Handle -r or --requirement (recursive requirements)
                if (line.startsWith("-r ") || line.startsWith("--requirement ")) {
                    def subFile = new File(requirementsFile.parent, line.split(/\s+/)[1])
                    if (subFile.exists()) {
                        dependencies.addAll(extractFromRequirements(subFile, type))
                    } else {
                        println "[WARN] Referenced requirements file not found: ${subFile.absolutePath}"
                    }
                    return
                }

                def name, version, actualType = type

                // Handle -e or --editable
                if (line.startsWith("-e ") || line.startsWith("--editable ")) {
                    def editablePath = line.split(/\s+/, 2)[1]
                    if (editablePath.contains("#egg=")) {
                        name = editablePath.split("#egg=")[1].split("&")[0]
                    } else {
                        name = editablePath.split("/").last().replaceAll(/\.git$/, "")
                    }
                    version = "editable"
                    actualType = "editable"
                } else {
                    // Parse standard requirement specifiers
                    // Handle various operators: ==, >=, <=, >, <, ~=, !=
                    def operators = ["==", ">=", "<=", "~=", "!=", ">", "<"]
                    def foundOperator = false

                    for (def op : operators) {
                        if (line.contains(op)) {
                            def parts = line.split(Pattern.quote(op), 2)
                            name = parts[0].trim()
                            // Fix: Remove the operator from version string
                            version = parts.size() > 1 ? parts[1].trim() : "unspecified"
                            foundOperator = true
                            break
                        }
                    }

                    if (!foundOperator) {
                        // No version specifier found
                        name = line.split(/[\s;]/)[0].trim()
                        version = "unspecified"
                    }

                    // Clean up package name (remove extras and environment markers)
                    if (name.contains("[")) {
                        name = name.split("\\[")[0]
                    }
                    if (name.contains(";")) {
                        name = name.split(";")[0].trim()
                    }
                }

                if (name && !name.isEmpty()) {
                    def dependency = new Dependency(
                            name: name,
                            version: version,
                            type: actualType,
                            source: requirementsFile.name,
                            registryUrls: [PYPI_REGISTRY]
                    )
                    dependencies << dependency
                    println "[INFO] Found dependency: ${dependency}"
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse line ${lineNumber} in ${requirementsFile.name}: '${line}' - ${e.message}"
            }
        }

        return dependencies
    }

    static List<Dependency> extractFromPipfile(File pipfile) {
        println "\n=== Analyzing Pipfile: ${pipfile.absolutePath} ==="

        if (!pipfile.exists()) {
            println "[ERROR] Pipfile doesn't exist"
            return []
        }

        def dependencies = []
        try {
            def content = pipfile.text
            def currentSection = null
            def inPackagesSection = false
            def inDevPackagesSection = false

            content.eachLine { line ->
                line = line.trim()

                if (line.startsWith("[packages]")) {
                    inPackagesSection = true
                    inDevPackagesSection = false
                    return
                } else if (line.startsWith("[dev-packages]")) {
                    inPackagesSection = false
                    inDevPackagesSection = true
                    return
                } else if (line.startsWith("[")) {
                    inPackagesSection = false
                    inDevPackagesSection = false
                    return
                }

                if ((inPackagesSection || inDevPackagesSection) && line.contains("=")) {
                    def parts = line.split("=", 2)
                    if (parts.length == 2) {
                        def name = parts[0].trim()
                        def versionSpec = parts[1].trim().replaceAll(/["']/, "")

                        if (versionSpec == "*") {
                            versionSpec = "unspecified"
                        }

                        def type = inDevPackagesSection ? "devDependencies" : "dependencies"

                        dependencies << new Dependency(
                                name: name,
                                version: versionSpec,
                                type: type,
                                source: "Pipfile",
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found ${type} dependency: ${name}==${versionSpec}"
                    }
                }
            }
        } catch (Exception e) {
            println "[ERROR] Failed to parse Pipfile: ${e.message}"
        }

        return dependencies
    }

    static List<Dependency> extractFromSetupPy(File setupFile) {
        println "\n=== Analyzing setup.py: ${setupFile.absolutePath} ==="

        if (!setupFile.exists()) {
            println "[ERROR] setup.py doesn't exist"
            return []
        }

        def dependencies = []
        try {
            def content = setupFile.text

            // Extract install_requires - handle multiline arrays
            def installRequiresPattern = /install_requires\s*=\s*\[(.*?)\]/
            def matcher = (content =~ /(?s)install_requires\s*=\s*\[(.*?)\]/)

            if (matcher.find()) {
                def depsString = matcher.group(1)
                def deps = depsString.split(',').collect {
                    it.trim().replaceAll(/['"\n\r]/, '').trim()
                }.findAll { it && !it.isEmpty() }

                deps.each { dep ->
                    def (name, version) = parseDependencyString(dep)
                    if (name) {
                        dependencies << new Dependency(
                                name: name,
                                version: version,
                                type: "dependencies",
                                source: "setup.py",
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found dependency: ${name}==${version}"
                    }
                }
            }

            // Extract extras_require
            def extrasPattern = /(?s)extras_require\s*=\s*\{(.*?)\}/
            def extrasMatcher = content =~ extrasPattern
            if (extrasMatcher.find()) {
                def extrasString = extrasMatcher.group(1)
                // Simple parsing of extras - this could be improved
                def extraLines = extrasString.split('\n')
                def currentExtra = null

                extraLines.each { line ->
                    line = line.trim()
                    if (line.contains(':')) {
                        def parts = line.split(':', 2)
                        currentExtra = parts[0].trim().replaceAll(/['"]/, '')
                        def depsLine = parts[1].trim()
                        if (depsLine.startsWith('[')) {
                            depsLine = depsLine.substring(1)
                        }
                        if (depsLine.endsWith(']')) {
                            depsLine = depsLine.substring(0, depsLine.length() - 1)
                        }

                        depsLine.split(',').each { dep ->
                            dep = dep.trim().replaceAll(/['"]/, '')
                            if (dep && !dep.isEmpty()) {
                                def (name, version) = parseDependencyString(dep)
                                if (name) {
                                    dependencies << new Dependency(
                                            name: name,
                                            version: version,
                                            type: "extras.${currentExtra}",
                                            source: "setup.py",
                                            registryUrls: [PYPI_REGISTRY]
                                    )
                                    println "[INFO] Found extra dependency: ${name}==${version} (${currentExtra})"
                                }
                            }
                        }
                    }
                }
            }

            // Extract tests_require
            def testsRequirePattern = /(?s)tests_require\s*=\s*\[(.*?)\]/
            def testsMatcher = content =~ testsRequirePattern
            if (testsMatcher.find()) {
                def depsString = testsMatcher.group(1)
                def deps = depsString.split(',').collect {
                    it.trim().replaceAll(/['"\n\r]/, '').trim()
                }.findAll { it && !it.isEmpty() }

                deps.each { dep ->
                    def (name, version) = parseDependencyString(dep)
                    if (name) {
                        dependencies << new Dependency(
                                name: name,
                                version: version,
                                type: "testDependencies",
                                source: "setup.py",
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found test dependency: ${name}==${version}"
                    }
                }
            }

        } catch (Exception e) {
            println "[ERROR] Failed to parse setup.py: ${e.message}"
        }

        return dependencies
    }

    static List<Dependency> extractFromSetupCfg(File setupCfgFile) {
        println "\n=== Analyzing setup.cfg: ${setupCfgFile.absolutePath} ==="

        if (!setupCfgFile.exists()) {
            println "[ERROR] setup.cfg doesn't exist"
            return []
        }

        def dependencies = []
        try {
            def content = setupCfgFile.text
            def lines = content.split('\n')
            def inInstallRequires = false
            def inExtrasRequire = false
            def currentExtra = null

            lines.each { line ->
                line = line.trim()

                if (line.startsWith('[')) {
                    inInstallRequires = false
                    inExtrasRequire = false
                    currentExtra = null
                    return
                }

                if (line.startsWith('install_requires')) {
                    inInstallRequires = true
                    return
                }

                if (line.startsWith('tests_require')) {
                    inInstallRequires = false
                    inExtrasRequire = true
                    currentExtra = "test"
                    return
                }

                if (line.matches(/.*_require$/)) {
                    inInstallRequires = false
                    inExtrasRequire = true
                    currentExtra = line.replace('_require', '')
                    return
                }

                if (inInstallRequires && line && !line.startsWith('#')) {
                    def (name, version) = parseDependencyString(line)
                    if (name) {
                        dependencies << new Dependency(
                                name: name,
                                version: version,
                                type: "dependencies",
                                source: "setup.cfg",
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found dependency: ${name}==${version}"
                    }
                }

                if (inExtrasRequire && currentExtra && line && !line.startsWith('#')) {
                    def (name, version) = parseDependencyString(line)
                    if (name) {
                        dependencies << new Dependency(
                                name: name,
                                version: version,
                                type: currentExtra == "test" ? "testDependencies" : "extras.${currentExtra}",
                                source: "setup.cfg",
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found ${currentExtra} dependency: ${name}==${version}"
                    }
                }
            }
        } catch (Exception e) {
            println "[ERROR] Failed to parse setup.cfg: ${e.message}"
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
            def content = pyprojectFile.text

            // Extract dependencies from [project] section (PEP 621)
            extractPep621Dependencies(content, dependencies)

            // Extract dependencies from [tool.poetry] section
            extractPoetryDependencies(content, dependencies)

            // Extract dependencies from [tool.setuptools] section
            extractSetuptoolsDependencies(content, dependencies)

        } catch (Exception e) {
            println "[ERROR] Failed to parse pyproject.toml: ${e.message}"
        }

        return dependencies
    }

    static void extractPep621Dependencies(String content, List<Dependency> dependencies) {
        try {
            // Extract main dependencies from [project] dependencies array
            def dependenciesPattern = /(?s)\[project\].*?dependencies\s*=\s*\[(.*?)\]/
            def matcher = content =~ dependenciesPattern
            if (matcher.find()) {
                def depsString = matcher.group(1)
                parseDependencyArray(depsString, "dependencies", "pyproject.toml", dependencies)
            }

            // Extract optional dependencies
            def optionalPattern = /(?s)\[project\.optional-dependencies\](.*?)(?=\[|\z)/
            def optionalMatcher = content =~ optionalPattern
            if (optionalMatcher.find()) {
                def optionalSection = optionalMatcher.group(1)
                def lines = optionalSection.split('\n')
                def currentGroup = null

                lines.each { line ->
                    line = line.trim()
                    if (line.contains('=') && line.contains('[')) {
                        def parts = line.split('=', 2)
                        currentGroup = parts[0].trim()
                        def depsStart = parts[1].trim()
                        if (depsStart.startsWith('[')) {
                            parseDependencyArray(depsStart, "extras.${currentGroup}", "pyproject.toml", dependencies)
                        }
                    }
                }
            }
        } catch (Exception e) {
            println "[WARN] Failed to extract PEP 621 dependencies: ${e.message}"
        }
    }

    static void extractPoetryDependencies(String content, List<Dependency> dependencies) {
        try {
            // Extract from [tool.poetry.dependencies]
            def poetryDepsPattern = /(?s)\[tool\.poetry\.dependencies\](.*?)(?=\[|\z)/
            def matcher = content =~ poetryDepsPattern
            if (matcher.find()) {
                def depsSection = matcher.group(1)
                parseTomlDependencies(depsSection, "dependencies", "pyproject.toml", dependencies)
            }

            // Extract from [tool.poetry.dev-dependencies]
            def poetryDevDepsPattern = /(?s)\[tool\.poetry\.dev-dependencies\](.*?)(?=\[|\z)/
            def devMatcher = content =~ poetryDevDepsPattern
            if (devMatcher.find()) {
                def devDepsSection = devMatcher.group(1)
                parseTomlDependencies(devDepsSection, "devDependencies", "pyproject.toml", dependencies)
            }

            // Extract from [tool.poetry.group.*.dependencies]
            def groupPattern = /(?s)\[tool\.poetry\.group\.([^.]+)\.dependencies\](.*?)(?=\[|\z)/
            def groupMatcher = content =~ groupPattern
            groupMatcher.each { match ->
                def groupName = match[1]
                def groupDepsSection = match[2]
                def type = groupName == "dev" ? "devDependencies" : "group.${groupName}"
                parseTomlDependencies(groupDepsSection, type, "pyproject.toml", dependencies)
            }
        } catch (Exception e) {
            println "[WARN] Failed to extract Poetry dependencies: ${e.message}"
        }
    }

    static void extractSetuptoolsDependencies(String content, List<Dependency> dependencies) {
        try {
            // Extract from [tool.setuptools] or [build-system]
            def setuptoolsPattern = /(?s)\[tool\.setuptools\].*?dependencies\s*=\s*\[(.*?)\]/
            def matcher = content =~ setuptoolsPattern
            if (matcher.find()) {
                def depsString = matcher.group(1)
                parseDependencyArray(depsString, "dependencies", "pyproject.toml", dependencies)
            }
        } catch (Exception e) {
            println "[WARN] Failed to extract setuptools dependencies: ${e.message}"
        }
    }

    static void parseTomlDependencies(String section, String type, String source, List<Dependency> dependencies) {
        def lines = section.split('\n')
        lines.each { line ->
            line = line.trim()
            if (line && !line.startsWith('#') && line.contains('=')) {
                def parts = line.split('=', 2)
                if (parts.length == 2) {
                    def name = parts[0].trim()
                    def versionSpec = parts[1].trim().replaceAll(/["']/, '')

                    // Handle complex version specifications
                    if (versionSpec.startsWith('{')) {
                        // Complex dependency specification
                        def versionMatch = versionSpec =~ /version\s*=\s*["']([^"']+)["']/
                        versionSpec = versionMatch.find() ? versionMatch.group(1) : "unspecified"
                    }

                    if (name != "python") { // Skip python version constraint
                        dependencies << new Dependency(
                                name: name,
                                version: versionSpec,
                                type: type,
                                source: source,
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found ${type} dependency: ${name}==${versionSpec}"
                    }
                }
            }
        }
    }

    static void parseDependencyArray(String depsString, String type, String source, List<Dependency> dependencies) {
        // Remove brackets and split by comma, handling multiline
        // Remove brackets and split by comma, handling multiline
        depsString = depsString.replaceAll(/[\[\]]/, '').trim()
        def deps = depsString.split(',').collect {
            it.trim().replaceAll(/['"\n\r]/, '').trim()
        }.findAll { it && !it.isEmpty() }

        deps.each { dep ->
            def (name, version) = parseDependencyString(dep)
            if (name) {
                dependencies << new Dependency(
                        name: name,
                        version: version,
                        type: type,
                        source: source,
                        registryUrls: [PYPI_REGISTRY]
                )
                // Fix the log message to not include the == operator
                println "[INFO] Found ${type} dependency: ${name} ${version}"
            }
        }
    }

    // Update the parseDependencyString method in PythonDependencyExtractor.groovy
    static List<String> parseDependencyString(String depString) {
        if (!depString || depString.trim().isEmpty()) {
            return [null, null]
        }

        depString = depString.trim()

        // Handle various operators
        def operators = ["==", ">=", "<=", "~=", "!=", ">", "<"]

        for (def op : operators) {
            if (depString.contains(op)) {
                def parts = depString.split(Pattern.quote(op), 2)
                def name = parts[0].trim()
                // Remove the operator from version string and clean it
                def version = parts.size() > 1 ? parts[1].trim().replaceAll(/["']/, '') : "unspecified"

                // Clean up package name (remove extras and environment markers)
                if (name.contains("[")) {
                    name = name.split("\\[")[0]
                }
                if (name.contains(";")) {
                    name = name.split(";")[0].trim()
                }

                return [name, version]
            }
        }

        // No version specifier found
        def name = depString.split(/[\s;\[]/, 2)[0].trim()
        return [name, "unspecified"]
    }

    static List<Dependency> extractFromCondaEnvironment(File envFile) {
        println "\n=== Analyzing Conda environment file: ${envFile.absolutePath} ==="

        if (!envFile.exists()) {
            println "[ERROR] Conda environment file doesn't exist"
            return []
        }

        def dependencies = []
        try {
            def content = envFile.text
            def inDependencies = false
            def inPipDependencies = false

            content.eachLine { line ->
                line = line.trim()

                if (line.startsWith("dependencies:")) {
                    inDependencies = true
                    return
                }

                if (inDependencies && line.startsWith("- pip:")) {
                    inPipDependencies = true
                    return
                }

                if (line.startsWith("-") && !line.startsWith("- pip:")) {
                    if (inPipDependencies) {
                        inPipDependencies = false
                    }

                    if (inDependencies) {
                        def depString = line.substring(1).trim()
                        def (name, version) = parseDependencyString(depString)
                        if (name) {
                            dependencies << new Dependency(
                                    name: name,
                                    version: version,
                                    type: "dependencies",
                                    source: envFile.name,
                                    registryUrls: [CONDA_REGISTRY]
                            )
                            println "[INFO] Found conda dependency: ${name}==${version}"
                        }
                    }
                } else if (inPipDependencies && line.startsWith("  - ")) {
                    def depString = line.substring(4).trim()
                    def (name, version) = parseDependencyString(depString)
                    if (name) {
                        dependencies << new Dependency(
                                name: name,
                                version: version,
                                type: "pip-dependencies",
                                source: envFile.name,
                                registryUrls: [PYPI_REGISTRY]
                        )
                        println "[INFO] Found conda pip dependency: ${name}==${version}"
                    }
                }

                if (!line.startsWith("-") && !line.startsWith(" ") && line.contains(":")) {
                    inDependencies = false
                    inPipDependencies = false
                }
            }
        } catch (Exception e) {
            println "[ERROR] Failed to parse conda environment file: ${e.message}"
        }

        return dependencies
    }

    static Map<String, String> detectVirtualEnvironments(File projectDir) {
        def venvs = [:]

        // Check for common virtual environment directories
        def venvDirs = [
                'venv', '.venv', 'env', '.env',
                'virtualenv', '.virtualenv', 'ENV'
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
        def files = projectDir.listFiles()?.collect { it.name } ?: []

        // Check for pip configuration files
        def pipFiles = [
                'pip.conf', '.pip/pip.conf',
                'pip.ini', '.pip/pip.ini'
        ]

        pipFiles.each { file ->
            if (new File(projectDir, file).exists()) {
                packageManagers << 'PIP'
                return true
            }
        }

        // Check for different package managers
        if ('Pipfile' in files || 'Pipfile.lock' in files) {
            packageManagers << 'PIPENV'
        }

        if ('pyproject.toml' in files) {
            def content = new File(projectDir, 'pyproject.toml').text
            if (content.contains('[tool.poetry]')) {
                packageManagers << 'POETRY'
            } else if (content.contains('[build-system]')) {
                packageManagers << 'PEP517'
            }
        }

        if ('environment.yml' in files || 'conda.yml' in files || 'environment.yaml' in files) {
            packageManagers << 'CONDA'
        }

        if ('setup.py' in files || 'setup.cfg' in files) {
            packageManagers << 'SETUPTOOLS'
        }

        if ('requirements.txt' in files || 'requirements.in' in files) {
            packageManagers << 'PIP'
        }

        return packageManagers.unique()
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
            def extraIndexMatch = content =~ /extra-index-url\s*=\s*(.+)$/
            if (extraIndexMatch.find()) {
                repos << extraIndexMatch.group(1).trim()
            }
        }

        // Check .pypirc
        def pypirc = new File(System.getProperty("user.home"), '.pypirc')
        if (pypirc.exists()) {
            def content = pypirc.text
            def repoMatches = content =~ /repository\s*=\s*(.+)$/
            repoMatches.each { match ->
                repos << match[1].trim()
            }
        }

        // Check pyproject.toml for repository sources
        def pyproject = new File(projectDir, 'pyproject.toml')
        if (pyproject.exists()) {
            try {
                def content = pyproject.text
                def sourcePattern = /(?s)\[\[tool\.poetry\.source\]\](.*?)(?=\[\[|\[|\z)/
                def sourceMatcher = content =~ sourcePattern
                sourceMatcher.each { match ->
                    def sourceSection = match[1]
                    def urlMatch = sourceSection =~ /url\s*=\s*["']([^"']+)["']/
                    if (urlMatch.find()) {
                        repos << urlMatch.group(1)
                    }
                }
            } catch (Exception e) {
                println "[WARN] Failed to parse pyproject.toml for repositories: ${e.message}"
            }
        }

        return repos.unique()
    }

    static void uploadDependency(String compiler, String runtimeVersion, String buildManager, String componentId, String branch, String sourceCodeUrl, List<Dependency> dependencies) {
        println "::debug::Pushing dependencies to Dependency Tracker API"
        def apiUrl = System.getenv('DEPENDENCY_TRACKER_API_URL')
        if (!apiUrl) {
            println "::warn::DEPENDENCY_TRACKER_API_URL environment variable is not set, using default URL."
            apiUrl = "http://localhost:8080/dependency-tracker/api/v1/components"
        }

        def apiToken = System.getenv('DEPENDENCY_TRACKER_API_TOKEN')
        if (!apiToken) {
            println "::warn::DEPENDENCY_TRACKER_API_TOKEN environment variable is not set"
            apiToken = ""
        }

        def name = extractNameFromSourceCodeUrl(sourceCodeUrl)

        def payload = [
                component   : [
                        name         : name,
                        sourceCodeUrl: sourceCodeUrl,
                        eimId        : '',
                ],
                componentId : componentId,
                branch      : branch,
                compiler    : compiler,
                runtimeInfo: [
                        version: runtimeVersion,
                        type   : 'PYTHON',
                ],
                language: 'PYTHON',
                buildManager: buildManager
        ]

        // Convert dependencies for the payload
        payload.dependencies = dependencies.collect { dep ->
            [
                    artefact: dep.name,
                    version : dep.version,
                    type: mapDependencyType(dep.type),
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
                if (apiToken) {
                    connection.setRequestProperty('Authorization', "Bearer ${apiToken}")
                }
                connection.setDoOutput(true)
                connection.setConnectTimeout(30000)
                connection.setReadTimeout(30000)

                // Write the payload
                connection.outputStream.withWriter { writer ->
                    writer << new JsonBuilder(payload).toString()
                }

                // Get the response
                def responseCode = connection.responseCode
                def responseBody = ""

                try {
                    responseBody = connection.inputStream.text
                } catch (IOException e) {
                    if (connection.errorStream) {
                        responseBody = connection.errorStream.text
                    }
                }

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
                    println "::warning::Request failed, retrying... (${retryCount + 1}/${maxRetries}): ${e.message}"
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

    static String mapDependencyType(String type) {
        switch (type) {
            case "devDependencies":
            case "testDependencies":
                return "development"
            case "editable":
                return "editable"
            case ~/extras\..*/:
                return "optional"
            case ~/group\..*/:
                return "group"
            default:
                return "runtime"
        }
    }

    static String extractNameFromSourceCodeUrl(String sourceCodeUrl) {
        if (!sourceCodeUrl) {
            return 'unknown-component'
        }

        try {
            // Handle common Git repository URL patterns
            def patterns = [
                    'github.com', 'gitlab.com', 'bitbucket.org',
                    'dev.azure.com', 'git.', 'gitlab.'
            ]

            for (def pattern : patterns) {
                if (sourceCodeUrl.contains(pattern)) {
                    def name = sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1)
                    return name.replaceAll(/\.git$/, '')
                }
            }

            // Default: use the last part of the URL
            return sourceCodeUrl.substring(sourceCodeUrl.lastIndexOf('/') + 1).replaceAll(/\.git$/, '')
        } catch (Exception e) {
            println "[WARN] Failed to extract name from sourceCodeUrl: ${sourceCodeUrl}"
            return 'unknown-component'
        }
    }

    static void main(String[] args) {
        if (args.length == 0) {
            println """
            Usage: groovy PythonDependencyExtractor.groovy <project_directory> [branch] [sourceCodeUrl] [compiler]
            Example: groovy PythonDependencyExtractor.groovy . main https://github.com/user/repo.git CPython
            """
            System.exit(1)
        }

        def projectDir = new File(args[0])
        if (!projectDir.exists()) {
            println "[ERROR] Project directory does not exist: ${projectDir.absolutePath}"
            System.exit(1)
        }

        if (!projectDir.isDirectory()) {
            projectDir = projectDir.getParentFile()
        }

        def branch = args.size() > 1 ? args[1] : "main"
        def sourceCodeUrl = args.size() > 2 ? args[2] : null
        def compiler = args.size() > 3 ? args[3] : null

        try {
            println "=== Python Dependency Extractor ==="
            println "Project Directory: ${projectDir.absolutePath}"
            println "Branch: ${branch}"
            println "Source Code URL: ${sourceCodeUrl ?: 'Not provided'}"
            println "Compiler: ${compiler}"

            // Detect component ID
            def componentId = extractComponentId(projectDir, sourceCodeUrl)
            println "Component ID: ${componentId}"

            // Detect runtime version
            def runtimeVersion = detectRuntime(projectDir)
            println "Runtime Version: ${runtimeVersion}"

            // Detect build manager
            def buildManager = detectBuildManager(projectDir)
            println "Build Manager: ${buildManager}"

            // Detect package managers
            def packageManagers = detectPackageManagers(projectDir)
            println "Package Managers: ${packageManagers.join(', ')}"

            // Detect virtual environments
            def venvs = detectVirtualEnvironments(projectDir)
            if (venvs) {
                println "Virtual Environments:"
                venvs.each { name, path ->
                    println "  - ${name}: ${path}"
                }
            }

            // Extract dependencies from various sources
            def dependencies = []

            // Check for requirements files (multiple variants)
            def requirementFiles = [
                    "requirements.txt", "requirements.in", "requirements-dev.txt",
                    "requirements-test.txt", "requirements-prod.txt", "dev-requirements.txt",
                    "test-requirements.txt", "prod-requirements.txt"
            ]

            requirementFiles.each { fileName ->
                def reqFile = new File(projectDir, fileName)
                if (reqFile.exists()) {
                    def type = fileName.contains("dev") ? "devDependencies" :
                            fileName.contains("test") ? "testDependencies" : "dependencies"
                    dependencies.addAll(extractFromRequirements(reqFile, type))
                }
            }

            // Check for Pipfile
            def pipfile = new File(projectDir, "Pipfile")
            if (pipfile.exists()) {
                dependencies.addAll(extractFromPipfile(pipfile))
            }

            // Check for setup.py
            def setupFile = new File(projectDir, "setup.py")
            if (setupFile.exists()) {
                dependencies.addAll(extractFromSetupPy(setupFile))
            }

            // Check for setup.cfg
            def setupCfgFile = new File(projectDir, "setup.cfg")
            if (setupCfgFile.exists()) {
                dependencies.addAll(extractFromSetupCfg(setupCfgFile))
            }

            // Check for pyproject.toml
            def pyprojectFile = new File(projectDir, "pyproject.toml")
            if (pyprojectFile.exists()) {
                dependencies.addAll(extractFromPyproject(pyprojectFile))
            }

            // Check for conda environment files
            def condaFiles = ["environment.yml", "environment.yaml", "conda.yml"]
            condaFiles.each { fileName ->
                def envFile = new File(projectDir, fileName)
                if (envFile.exists()) {
                    dependencies.addAll(extractFromCondaEnvironment(envFile))
                }
            }

            // Remove duplicates based on name and source
            dependencies = dependencies.unique { a, b ->
                a.name <=> b.name ?: a.source <=> b.source
            }

            println "\n=== Summary ==="
            println "Total dependencies found: ${dependencies.size()}"


            if(runtimeVersion == null || runtimeVersion == "UNKNOWN") {
                // get it from compiler, the compiler is usually like /usr/var/python/3.12.5
                runtimeVersion = compiler.split("/").last()
            }

            // Upload dependencies
            uploadDependency(compiler, runtimeVersion, buildManager, componentId, branch, sourceCodeUrl, dependencies)

            // Group dependencies by type for clear output
            def grouped = dependencies.groupBy { it.type }

            println "\n=== Dependencies by Type ==="
            grouped.each { type, deps ->
                println "\n${type.toUpperCase()} (${deps.size()}):"
                deps.sort { it.name }.each { dep ->
                    println "  - ${dep.name} ${dep.version} (from ${dep.source})"
                }
            }

            println "\n=== Dependencies by Source ==="
            def groupedBySource = dependencies.groupBy { it.source }
            groupedBySource.each { source, deps ->
                println "\n${source} (${deps.size()}):"
                deps.sort { it.name }.each { dep ->
                    println "  - ${dep.name} ${dep.version} (${dep.type})"
                }
            }

        } catch (Exception e) {
            println "[ERROR] Failed to process project: ${e.message}"
            e.printStackTrace()
            System.exit(1)
        }
    }
}