let
  tag = "22.11";
  nixpkgs = fetchTarball "https://github.com/NixOS/nixpkgs/archive/refs/tags/${tag}.tar.gz";
  pkgs = import nixpkgs {
    config = { allowUnfree = true; };
  };
  rush = pkgs.writeShellScriptBin "rush" ''
    npx @microsoft/rush "$@";
  '';
in with pkgs; [
  # Runtimes
  nodejs-16_x
  babashka
  clojure

  cowsay
  
  # Scripts
  rush

  # keep this line if you use bash
  bashInteractive
]
