node {

  currentBuild.result = "SUCCESS"

  try {

    stage 'Checkout'

      checkout scm

    stage 'Test'

      sh "boot --no-colors ci"

    stage 'Build'

      sh "boot build -e dev-builds"
      notifySuccessful()

    stage 'Deploy'

      sh "./bin/deploy -b dev.zetawar.com"

  } catch (err) {

      currentBuild.result = "FAILURE"

      throw err

  }
}

def notifySuccessful() {
  emailext (
      subject: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: """<p>SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
        <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
      to: "djwhitt@gmail.com"
    )
}