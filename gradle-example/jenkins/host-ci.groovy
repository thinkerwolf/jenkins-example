pipeline {
    // Agent定义
    agent {
        // 流水线运行在基于Kubernetes POD的动态agent中
        kubernetes {
            // PodTemplate，配置位置在：系统管理->Clouds->kubernetes->Pod Templates
            inheritFrom 'gradle-jdk17'
        }
    }

    // 使用的工具定义，配置位置在：系统管理->全局工具配置
    tools {
        // 使用全局工具配置中名为gradle-8.8的gradle
        gradle 'gradle-8.8'
    }

    // 变量定义
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
                        // 将war、jar、tar等交付物上传到oss制品库
                        ossUpload ossId: 'oss', pathPrefix: '${JOB_NAME}/${BUILD_NUMBER}/', includes: '$BASE_DIR/build/libs/*.jar', excludes: '$BASE_DIR/build/libs/*-plain.jar'
                    }
                }
            }
        }
    }
}
