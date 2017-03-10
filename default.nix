with import <nixpkgs> {}; {
  sdlEnv = stdenv.mkDerivation {
    name = "zetawar";
    buildInputs = [ boot git nodejs phantomjs2 s3cmd stdenv ];
    LD_LIBRARY_PATH="${stdenv.cc.cc.lib}/lib";
  };
}
