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
                    // 从蓝鲸CMDB中获取 业务->集群->模块 下的主机IP列表，
                    // 运行完成后，默认将内网IP存储在BK_INNER_IPS环境变量，外网IP存储在BK_OUTER_IPS环境变量
                    bkCC baseUrl: 'http://paas.bluebell007.xyz', bkUsername: '${BK_USER}', bkAppCode: '${BK_CRED_USR}', bkAppSecret: '${BK_CRED_PSW}', bkBiz: 'Goose', bkSet: 'COMMON_SERVICE', bkModules: '370'
                }
            }
        }

        stage('Deploy') {
            // 此阶段运行在特定Agent
            agent {
                // 通过label标签将此阶段固定在应用所在AWS的Static Agent上运行，进行内网部署
                label 'agent-sit'
            }
            steps {
                // 打印要部署的主机IP列表
                sh 'echo "$BK_INNER_IPS"'
                // 从OSS制品库下载制品
                ossDownload ossId: 'oss', path: '${OSS_PATH}/${CI_BUILD_NUMBER}/'
                sh 'ls -hl'
                // TODO 执行应用的部署脚本
            }
        }
    }
}
