package main

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
)

//go:generate goversioninfo

func main() {
	fmt.Println("Battleships PC client launcher")
	path, err := os.Executable()
	if err == nil {
		base_folder := filepath.Dir(path)
		cmd := exec.Command(base_folder+"\\jre\\bin\\javaw.exe", "-Dsun.java2d.opengl=true", "-jar", base_folder+"\\battleships-resurrection.jar")
		err = cmd.Start()
		if err != nil {
			log.Fatal(err)
		}
	}
}
