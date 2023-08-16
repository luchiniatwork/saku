{
  description = "A flake file for this repo";

  inputs = {
    nixpkgs.url = github:NixOS/nixpkgs/nixos-23.05;
    flake-utils.url = github:numtide/flake-utils;
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config = {
              allowUnfree = true;
            };
          };
          rush = pkgs.writeShellScriptBin "rush" ''
            npx @microsoft/rush "$@";
          '';
          pnpm = pkgs.writeShellScriptBin "pnpm" ''
            npx pnpm "$@";
          '';
        in
          {
            devShells.default = pkgs.mkShell {
              buildInputs = with pkgs; [
                # Runtimes
                nodejs_20
                babashka
                clojure

                # Scripts
                rush
                pnpm

                # keep this line if you use bash
                bashInteractive
              ];
            };
          }
      );
}
