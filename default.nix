with import <nixpkgs> {}; {
  sdlEnv = stdenv.mkDerivation {
    name = "zetawar";
    buildInputs = [ boot nodejs openjdk stdenv ];
    LD_LIBRARY_PATH="${stdenv.cc.cc.lib}/lib";
  };
}
