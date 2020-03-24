![](https://img.shields.io/github/release/klugem/watchdog.svg) ![](https://img.shields.io/github/workflow/status/klugem/watchdog/mvn_build)

### MANUAL
The most recent version of the manual can be found online at https://www.bio.ifi.lmu.de/files/Software/Watchdog/Watchdog-manual.html and as HTML and PDF version in the documentation folder.

### REQUIREMENTS
Watchdog is written in Java and requires JDK 11 or higher. Oracle provides an installation guide for Windows, Linux and macOS at https://docs.oracle.com/en/java/javase/11/install/overview-jdk-installation.html.

The GUI of the workflow designer and the moduleMaker depend on the JavaFX SDK 11 or higher, which can be obtained from https://gluonhq.com/products/javafx/. An installation guide is provided here: https://openjfx.io/openjfx-docs/
Alternatively, the JavaFX version obtained with the Maven build script can be used that is located in _jars/libs/modules/_.

### INSTALLATION
- Manually:
    - Download a release from https://github.com/klugem/watchdog/releases
    - Extract the provided archive into a folder of your choice
    - Modules can be obtained from https://github.com/watchdog-wms/watchdog-wms-modules and moved manually into the modules directory or by calling `./modules/downloadCommunityModules.sh` from the Watchdog directory 

- Via conda:
    - `conda install -c bioconda -c conda-forge watchdog-wms`
- Via docker:
    - `docker pull klugem/watchdog-wms`

In case of conda or docker installation, the binaries are named watchdog-cmd and watchdog-gui while the rest of the files are located in _${PREFIX}/share/watchdog-wms-${VERSION}_.

### RUN WATCHDOG
The distributed jar files are build for Java 11 and are called internally by the bash scripts. In order to run a xml-file using the command line tool, call

`./watchdog.sh -x xml-file` # (or java -jar _jars/watchdog.jar_ -x xml-file)

To call the jar file directly is not recommended as CTRL+C signal will be forwarded to all child processes, which might cause errors on locally running tasks.

The workflow designer can be started using 
`./workflowDesigner.sh` # (or java -jar _jars/WatchdogDesigner.jar_)

The workflow designer depends on the Javafx SDK 11 or higher. The bash script will try to identify the installation location of the Javafx SDK automatically.

### JAR FILES
- watchdog.jar - command-line tool that executes Watchdog workflows
- watchdogDesigner.jar - graphical user interface for workflow design and execution
- docuTemplateExtractor.jar - generates templates for module documentation
- refBookGenerator.jar - creates a module reference book based on a set of modules
- reportGenerator.jar - basic reporting of steps performed during execution of a workflow
- moduleValidator.jar - command-line tool that can be used to verify integrity of modules
- workflowValidator.jar - command-line tool that can be used to verify integrity of workflows
- moduleMaker.jar - provides a graphical user interface for module creation

All jar files except moduleMaker.jar are available in the _jars/_ subdirectory of the Watchdog installation folder. The ModuleMaker is not shipped with Watchdog but can be obtained by running `./helper_scripts/downloadModuleMaker.sh` located in the Watchdog installation directory. See https://github.com/watchdog-wms/moduleMaker/blob/master/README.md for more information.

More information on how to use these programmes can be found in the [manual](https://klugem.github.io/watchdog/Watchdog-manual.html#JARs) in section 6.

### GETTING STARTED

Once Watchdog is correctly installed, you can run example workflows shipped with Watchdog. To configure them run `./helper_scripts/configureExamples.sh -i /path/to/install/folder/of/watchdog [-m your@mail-adress.com]`. Afterwards, the example workflows are located in _/path/to/install/folder/of/watchdog/examples_ and can be started using `./watchdog.sh -x path2/xml-file.xml`.

- example_basic_sleep.xml - basic example with one task to show XML workflow structure
- example_dependencies.xml - workflow with dependencies between tasks
- example_execution_environments.xml - workflow using different execution environments (requires modifications)
- example_process_blocks.xml - shows how to work with process sequences, folders and tables 
- example_task_actions.xml - introduces task actions using the example of a file copy action
- example_checkers.xml - shows how to use a custom success checker
- example_docker.xml - uses a module that internally uses a docker image containing bowtie2
- example_include.xml - shows how to use additional module folders
- example_simple_calculations.xml - usage of simple mathematical calculations within a workflow
- example_constant_replacement.xml - shows to to use constants in workflows
- example_environment_variables.xml - setting environment variables
- example_mail_notification.xml - example with mail notifications on completed subtasks and checkpoints
- example_streams.xml - rediction of stdout and stderr streams
- workflow1_basic_information_extraction.xml - simple workflow that extracts information from files using basic UNIX tools
- workflow2_differential_gene_expression.xml - workflow performing a differential gene expression analysis on an example data set (needs bwa and ContextMap2 to be installed and configured)

More information on these example workflows can be found in the [manual](https://klugem.github.io/watchdog/Watchdog-manual.html#getting_started) in section 3 and 4.

An introduction on how to [analyse replicate data](https://klugem.github.io/watchdog/ReplicateAnalysis_Overview.pdf) or how use the [workflow designer (GUI)](https://klugem.github.io/watchdog/WorkflowDesigner_Overview.pdf) can be found in the _documentation/_ folder.

### WATCHDOG COMMUNITY
Two repositories on Github under the watchdog-wms organization (https://github.com/watchdog-wms/) are dedicated for sharing modules and workflows by other users: 
- modules: https://github.com/watchdog-wms/watchdog-wms-modules
- workflows: https://github.com/watchdog-wms/watchdog-wms-workflows

#### CONTACT
If you have any questions or suggestions, please feel free to contact me: michael.kluge@bio.ifi.lmu.de
In case of bugs or feature requests, feel free to create an issues at https://github.com/klugem/watchdog/issues

### LICENCE
Watchdog is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Watchdog is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Watchdog.  If not, see <http://www.gnu.org/licenses/>.

Licenses of libraries Watchdog depends on can be found in _jars/libs/_.
