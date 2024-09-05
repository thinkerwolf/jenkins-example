pipeline {
    agent {
        kubernetes {
            inheritFrom 'jdk17'
        }
    }

    tools {
        gradle 'gradle-8.8'
    }

    environment {
        GROUP_ID = 'tech.bitkernel.devops'
        PROJ_PATH = 'gradle-example'
        NEXUS_REPO = 'raw-wa'
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
                    cd $PROJ_PATH
                    gradle -v
                    gradle build -x test --stacktrace --scan --refresh-dependencies -b build.gradle
                    
                    # 将changelog目录下的nacos和数据库变更上传到制品库
                    tar -czf changelog.tar.gz changelog
                    """
                        // 上传至nexus
                        nexusArtifactPublish(
                                serverId: 'nexus',
                                repository: "${NEXUS_REPO}",
                                groupId: "${GROUP_ID}",
                                artifactId: "gradle-example",
                                version: version,
                                includes: "${PROJ_PATH}/changelog.tar.gz")
                    }
                }
            }
        }
    }
}

