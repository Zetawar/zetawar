node {

  currentBuild.result = "SUCCESS"

  try {

    stage 'Checkout'

      checkout scm

    stage 'Test'

      sh "boot --no-colors ci"

  } catch (err) {

      currentBuild.result = "FAILURE"

      throw err

  }
}
