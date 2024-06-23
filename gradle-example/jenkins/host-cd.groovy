pipeline {
    agent {
        kubernetes {
            inheritFrom 'default'
        }
    }

    environment {
        OSS_PATH = 'example/host-ci-example'
        CI_BUILD_NUMBER = 10
        BK_CRED = credentials('blueking-cred')
        BK_USER = 'shawn.lu'
    }

    stages {
        stage('Blueking') {
            steps {
                container('jnlp') {
                    bkCC baseUrl: 'http://paas.bluebell007.xyz', bkUsername: '${BK_USER}', bkAppCode: '${BK_CRED_USR}', bkAppSecret: '${BK_CRED_PSW}', bkBiz: 'Goose', bkSet: 'COMMON_SERVICE', bkModules: '370'
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'agent-sit'
            }
            steps {
                sh 'echo "$BK_INNER_IPS"'
                ossDownload ossId: 'oss', path: '${OSS_PATH}/${CI_BUILD_NUMBER}/'
                sh 'ls -hl'
                // TODO 执行部署脚本
            }
        }
    }
}
