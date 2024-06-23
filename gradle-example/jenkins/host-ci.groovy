pipeline {
    agent {
        kubernetes {
            inheritFrom 'gradle-jdk17'
        }
    }

    tools {
        gradle 'gradle-8.8'
    }

    environment {
        BASE_DIR = 'gradle-example'
        gitCredential = 'github-thinkerwolf'
    }

    stages {
        stage('SCM checkout') {
            steps {
                container('git') {
                    git branch: 'main', credentialsId: gitCredential, url: 'https://github.com/thinkerwolf/jenkins-example.git'
                }
            }
        }

        stage('Build') {
            steps {
                withGradle {
                    container('jdk') {
                        sh 'gradle -v'
                        sh 'gradle build -x test --stacktrace --scan --refresh-dependencies -b $BASE_DIR/build.gradle'
                        sh 'ls -hl $BASE_DIR/build'

                        // 将war或jar包上传到oss制品库
                        ossUpload ossId: 'oss', includes: '$BASE_DIR/build/libs/*.jar', excludes: '$BASE_DIR/build/libs/*-plain.jar', pathPrefix: '${JOB_NAME}/${BUILD_NUMBER}/'
                    }
                }
            }
        }
    }

}
