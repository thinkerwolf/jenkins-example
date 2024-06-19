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
        ARTIFACT_NAME = 'host-example'
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
                        sh 'gradle build --no-daemon -x test --refresh-dependencies -b gradle-example/build.gradle'
                        sh 'ls -hl gradle-example/build'
                        archiveArtifacts artifacts: 'gradle-example/build/libs/*.jar', excludes: 'gradle-example/build/libs/*-plain.jar'
                    }
                }
            }
        }

        // stage('Deploy') {}
    }

}
