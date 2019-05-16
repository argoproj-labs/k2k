# Converting Ksonnet to Kustomize

**_WARNING_**: The conversion from Ksonnet to Kustomize causes a short downtime in each environment as it is being restacked. 
For service with a DR or that can tolerate the downtime the steps below as-is. 
For downtime intolerant service that don’t have a DR follow the ALTERNATE steps when given.
The ALTERNATE behaviour takes longer and consumes a lot of extra resources.

1. PREREQUISITES: 
	1. ArgoCD installed, as documented [here](https://argoproj.github.io/argo-cd/getting_started/#2-download-argo-cd-cli).
	1. Ksonnet installed, as documented [here](https://ksonnet.io/get-started/).
	1. Kustomize installed, as documented [here](https://github.com/kubernetes-sigs/kustomize/blob/master/docs/INSTALL.md).
	1. GitHub configured with a personal access token, as documented [here](https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line).
	1. Get Conversion Tool:
		1. Get _k2k.jar_ [here](https://github.com/argoproj-labs/ksonnet2kustomize/releases).
		1. Put the _k2k.jar_ in the same directory as the Ksonnet 'app.yaml' file (Don’t commit. Just put as a local copy)
		<BR>NOTE: The ```k2k``` application will not work if it is run from any other location.
1. TRANSLATE KSONNET TO KUSTOMIZE: 
	1. Run ```java -jar k2k.jar translate <serviceName>```
		<br>where:
		<br>&nbsp;&nbsp;_serviceName_ is your application’s name as given in the Ksonnet configuration files.
		<br>_ALTERNATE_: add the ```–nodowntime``` option to the command line.
		<br>NOTE: On occasion this fails when the program attempts to run ```ks show``` on an environment with an incomplete definition that is not in use.
		It cannot distinguish between this and a real failure, and so stops. In this case run:
			<br>1. ```java -jar k2k.jar clean-kustomize```
			<br>2. ```rm -fr environments/pr``` or whatever environment is giving problems.
			<br>3. Re-run the translation.
	1. This creates an ```app-base``` directory with Kustomize files, and it adds Kustomize files to each of each subdirectory in the ```environments``` directory.
	1. [Optional] Validate:
		<br>NOTE: When converting to Kustomize the 'kind', 'apiversion', and 'metadata.name' are used as the primary key of the resource, and therefore cannot be changed when inheriting, though Kustomize allows prefixing during inheritence. This is the reason when the resources names change during the conversion, and the reason for the restacking.
		1. Assuming your first enviornment is ```qal```:
		1. ```cd``` to ```environments/qal``` or whatever your first environment is.
		1. Run ```ks show qal >ks-manifest.yaml```
		1. Run ```kustomize build >kustomize-manifest.yaml```
		1. Run ```diff ks-manifest.yaml kustomize-manifest.yaml >diff```
		1. Inspect the ```diff``` file.
		1. Run ```kubectl apply -k . --dry-run=true``` to have Kubernetes validate the converted syntax.
		1. When done validating this environment, delete the ks-manifest.yaml, kustomize-manifest.yaml, and diff files. 
		<br>ALTERNATE: leave the ks-manifest.yaml.
		1. ALTERNATE: Edit the Deployment resource patch yaml file and changes the DNS hostname of the ALB by adding a "-1" at the end of the name.
		1. ALTERNATE: If this is a production environment, using IKSM create a new cert that contains all the names in the current plus the new name we created by adding "-1".
		1. ALTERNATE: Update the Ingress resource patch yaml file with the new cert ARN.
		1. Repeat these steps for each of your other environments.
	1. Perform a git add, git commit and git push for the files in ```app-base/*``` and the files ```environments/*/*.yaml```.
	This is safe as these are new files and ArgoCD does not begin to use them until it is commanded to switch from Ksonnet to Kustomize.
1. UPDATE ARGOCD:
	1. Login to argocd. For example, if using SSO, use the command: ```argocd login {argocd_server} --sso``` replacing _argocd_server_ with your server.
	1. To switch ArgoCD to use Kustomize, run ```java -jar k2k.jar argocd2kustomize <gitRepo> [<targetEnv>]```
	<br>where:
	<br>&nbsp;&nbsp;_gitRepo_ is the url to the repo containing the commited Kustomize file.
	<br>&nbsp;&nbsp;_targetEnv_ is an OPTIONAL argument to limit the switch to one environment.
	2. If for any reason you would like to switch back, run ```java -jar k2k.jar argocd2ksonnet <gitRepo> [<targetEnv>]```
	3. Verify in ArgoCd that the ```sync``` completes successfully and your application operates as expected.
1.	UPDATE PIPELINE:
	1. Image Update: Update the pipeline to use ```/usr/local/bin/kustomize edit set image``` command instead of ```/usr/local/bin/ks param set``` command. 
	1. Argo CD App Create (optional): if done in pipeline update to use:
		1. ```--path environments/${envName} -- dest-server ${cluster} --dest-namespace ${namespace}``` instead of
		```--path . --env ${envName}```.
		<br>the dest-server and dest-namespace can be found in the _app.yaml_ file. 
1.	ALTERNATE: SWITCH TRAFIC.
	1. Switch the endpoint to the new ALB DNS names (with "-1") and verify operation.
1.	CLEANUP:
	1. Verify everything is working as it was and that you no longer want to retain the Ksonnet fallback.
	1. ALTERNATE: In each environment directory in the kustomization.yaml file delete the line declaring the ks-manifest.yaml file as a resource and remove the ks-manifest.yaml file itself. 
	1. ALTERNATE: Commit and push.
	1. ALTERNATE: To remove the old Ksonnet named resource, run again ```java -jar k2k.jar argocd2ksonnet <gitRepo>```
	<br>where:
	<br>&nbsp;&nbsp;_jenkinsfilePath_ is the path to the Jenkinsfile in your application’s code repository.
	2. Run ```java -jar k2k.jar clean-ksonnet``` to remove all the no longer needed Ksonnet files. Once this is done, it is no longer possible to run any other ```k2k``` command.
	3. Commit and push.

















