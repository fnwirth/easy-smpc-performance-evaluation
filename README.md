# Performance evaluation of the software EasySMPC

> This repository allows to reproduce the performance evaluation of the software [EasySMPC](https://github.com/prasser/easy-smpc) in a dockerized form with the e-mail server  [iRedMail](https://www.iredmail.org/). The steps are described below.
---

## Steps to reproduce the performance evaluation
1. Clone this repository with the command `clone https://github.com/fnwirth/easy-smpc-performance-evaluation`.
1. Build the program with the command `mvn clean package`. The two warnings regarding the import of easy-smpc.jar can be ignored.
1. Create the docker image by changing to the folder *docker* and executing the script `createDockerImage.sh` or `createDockerImage.bat` respectively
1. Start the docker image with the script `startEvaluation.sh` or `startEvaluation.bat`. To change the network delay, the parameter *TC_DELAY_MS* can adapted before starting the script.
1. The performance evaluation is running and can be inspected with the command `docker logs easy-eval`. The results can be accessed in the file */root/easy-smpc/result.csv* within the container e.g. by copying the file to the host with the command `docker cp easy-eval:/root/easy-smpc/performanceEvaluation.csv .`

## Contact
See [README page](https://github.com/prasser/easy-smpc/edit/master/README.md) of EasySMPC

## License
This software is licensed under the Apache License 2.0. The full text is accessible in the LICENSE file. EasySMPC itself has several dependencies whose license files are listed in the github [README page](https://github.com/prasser/easy-smpc/edit/master/README.md).
The dockerfile uses the docker image provided by iRedMail. See the [documentation](https://github.com/iredmail/dockerized) of the image for license details.