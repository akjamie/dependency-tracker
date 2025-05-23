# Dependency Tracker - Component Dependency Evergreening System

## Overview
Dependency Tracker is an automated system designed to manage and track component dependencies across an organization. It helps maintain up-to-date dependencies by providing visibility, automation, and governance for dependency management.

## Key Features

### 1. Dependency Extraction
- Automated extraction of dependencies from various project configuration files:
  - Maven (`pom.xml`)
  - Python (`requirements.txt`)
  - Node.js (`package.json`)
- Groovy scripts embedded in component build pipelines
- Real-time dependency data upload to the tracking system
- Support for multiple build tools and languages

### 2. Evergreening Rules Management
- Centralized management of dependency upgrade rules
- Support for forced upgrade policies (e.g., JDK 21 by July 2025)
- Framework version requirements (e.g., Spring Boot 3.4.5 by end of 2025)
- Rule-based dependency management
- Policy enforcement and tracking

### 3. Reporting and Communication
- Department-level dependency status reporting
- Automated upgrade reminders
- Component-level dependency analysis
- Version distribution tracking
- Build tool usage statistics

### 4. Automated Upgrade Support
- Automated dependency version updates
- Pull Request generation for dependency updates
- Integration with Git repositories
- Pending review workflow for application teams

## Technical Architecture

### Data Model
The system uses MongoDB to store component dependency information with the following structure:

```json
{
  "metadata": {
    "name": "component-name",
    "eimId": "eim-id",
    "sourceCodeUrl": "github-url"
  },
  "componentId": "org.example:component",
  "branch": "main",
  "compiler": "linux/jdk11",
  "runtimeVersion": "JDK 11",
  "language": "JAVA",
  "buildManager": "MAVEN",
  "checksum": "hash",
  "dependencies": [
    {
      "artefact": "dependency-name",
      "version": "version-number",
      "type": "dependency-type"
    }
  ],
  "lastUpdatedAt": "timestamp",
  "createdAt": "timestamp"
}
```

### API Endpoints

#### Version Distribution Facet
- Endpoint: `GET /api/v1/dependencies/facets/versions`
- Provides version distribution information for:
  - Java versions
  - Python versions
  - Node.js versions
  - Spring Boot versions
  - Frontend frameworks (React, Angular, Vue)
- Includes percentage calculations within each category

## Getting Started

### Prerequisites
- Java 17 or higher
- MongoDB
- Maven
- Groovy

### Installation
1. Clone the repository
2. Configure MongoDB connection
3. Build the project:
   ```bash
   mvn clean install
   ```
4. Run the application:
   ```bash
   java -jar target/dependency-tracker.jar
   ```

### Integration
1. Add the dependency extractor Groovy script to your build pipeline
2. Configure the API endpoint in your build configuration
3. Set up evergreening rules in the system
4. Configure notification settings

## Usage

### Adding Evergreening Rules
1. Access the rules management interface
2. Define upgrade requirements
3. Set target dates and versions
4. Configure notification preferences

### Monitoring Dependencies
1. View department-level reports
2. Check component-specific dependency status
3. Review upgrade recommendations
4. Track compliance with evergreening rules

### Automated Updates
1. Enable automated PR generation
2. Configure review workflows
3. Set up testing requirements
4. Monitor update status

## Contributing
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License
[Add your license information here]

## Support
[Add support contact information here]
