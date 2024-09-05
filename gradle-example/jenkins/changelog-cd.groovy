pipeline {
    agent {
        label 'example'
    }

    environment {
        NEXUS_REPO = 'raw-wa'
    }

    stages {
        stage('Config') {
            steps {
                script {
                    def gav = "${DEPLOY_ARTIFACT}".split(':')
                    def groupId = gav[0]
                    def artifactId = gav[1]
                    def version = gav[2]

                    nexusArtifactDownload(
                            serverId: 'nexus',
                            repository: "${NEXUS_REPO}",
                            groupId: groupId,
                            artifactId: artifactId,
                            version: version,
                            location: 'changelog.tar.gz')
                    sh '''
                tar -xvf changelog.tar.gz 
                '''

                    // 执行Nacos配置变更，读取 changeLog 文件，获取changeSet
                    def nacosChangeSet = nacosChangeSetGet(nacosId: 'default', changeLogFile: 'changelog/nacos/changelog-root.yaml')
                    // 弹出界面预览配置前后修改比对
                    def alterResult = input(message: 'Nacos Config Edit', parameters: [nacosConfigAlter(items: nacosChangeSet['changes'])])
                    // 应用 changeSet，刷入nacos配置
                    nacosChangeSetApply(nacosId: "default", changeSetId: nacosChangeSet['id'], items: alterResult['values'])

                    // 执行SQL变更
                    sh 'liquibase update --defaultsFile=liquibase.properties --changelog-file=changelog/database/changelog-root.yaml'
                }
            }
        }
    }

}
