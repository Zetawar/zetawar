node {

  currentBuild.result = "SUCCESS"

  try {
    stage 'Checkout'

      git url: "https://github.com/Zetawar/zetawar.git"

    stage 'Test'

      sh "/usr/local/bin/boot --no-colors ci"

  } catch (err) {

      currentBuild.result = "FAILURE"

      throw err

  }
}
