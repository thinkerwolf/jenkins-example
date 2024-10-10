pipeline {
    agent {
        kubernetes {
            inheritFrom 'jdk17'
        }
    }

    tools {
        gradle 'gradle-8.8'
        maven 'maven-3.9'
    }

    environment {
        GROUP_ID = 'tech.bitkernel.devops'
        NEXUS_REPO = 'raw-op'
    }

    stages {
        stage('SCM Checkout') {
            steps {
                container('git') {
                    git branch: 'main', url: 'https://github.com/thinkerwolf/jenkins-example.git'
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def version = new Date().format('yyyyMMddHHmmss') + "-${env.BUILD_ID}"

                    container('jdk') {
                        sh """
                        cd gradle-example
                        gradle -v
                        gradle build -x test --stacktrace --scan --refresh-dependencies -b build.gradle
                        
                        # 将changelog目录下的nacos和数据库变更上传到制品库
                        tar -czf changelog.tar.gz changelog
                        """
                        nexusArtifactPublish(
                                serverId: 'nexus',
                                repository: "${NEXUS_REPO}",
                                groupId: "${GROUP_ID}",
                                artifactId: "gradle-example",
                                version: version,
                                includes: "gradle-example/changelog.tar.gz")

                        sh """
                        cd maven-example
                        mvn package -Dmaven.test.skip=true
                        # 将changelog目录下的nacos和数据库变更上传到制品库
                        tar -czf changelog.tar.gz changelog
                        """
                        nexusArtifactPublish(
                                serverId: 'nexus',
                                repository: "${NEXUS_REPO}",
                                groupId: "${GROUP_ID}",
                                artifactId: "maven-example",
                                version: version,
                                includes: "maven-example/changelog.tar.gz")
                    }
                }
            }
        }
    }
}

