node {

  currentBuild.result = "SUCCESS"

  try {

    stage 'Checkout'

      checkout scm

    stage 'Test'

      sh "boot --no-colors ci"

    stage 'Build'

      sh "boot build -e dev-builds"

    stage 'Deploy'

      sh "./bin/deploy -b dev.zetawar.com"

  } catch (err) {

      currentBuild.result = "FAILURE"

      throw err

  }
}
