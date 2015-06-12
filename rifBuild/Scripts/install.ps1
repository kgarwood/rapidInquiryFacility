# ************************************************************************
#
# GIT Header
#
# $Format:Git ID: (%h) %ci$
# $Id: e96a6b0aa1ba85325e1b7b0e57163d2b7707440b $
# Version hash: $Format:%H$
#
# Description:
#
# Rapid Enquiry Facility (RIF) - Helper script to install a file in windows 
#
# Copyright:
#
# The Rapid Inquiry Facility (RIF) is an automated tool devised by SAHSU 
# that rapidly addresses epidemiological and public health questions using 
# routinely collected health and population data and generates standardised 
# rates and relative risks for any given health outcome, for specified age 
# and year ranges, for any given geographical area.
#
# Copyright 2014 Imperial College London, developed by the Small Area
# Health Statistics Unit. The work of the Small Area Health Statistics Unit 
# is funded by the Public Health England as part of the MRC-PHE Centre for 
# Environment and Health. Funding for this project has also been received 
# from the Centers for Disease Control and Prevention.  
#
# This file is part of the Rapid Inquiry Facility (RIF) project.
# RIF is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# RIF is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with RIF. If not, see <http://www.gnu.org/licenses/>; or write 
# to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
# Boston, MA 02110-1301 USA
#
# Author:
#
# Peter Hambly, SAHSU
#
# Args 0: File to be installed. If file is a zip file, unzipped to install directory
# Args 1: Install directory, must exist 
# Args 2: Unpack tree base directory [OPTIONAL, tomcat auto unpack only]
# Args 3: Log file (privileged second run)
# Args 4: Error file (privileged second run)
#
# Check runs as administrator
#
Param(
	[ValidateNotNullOrEmpty()][string]$installFile,
	[ValidateNotNullOrEmpty()][string]$installDir,
	[string]$installTree,
	[string]$log,
	[string]$err
)
#
# Setup log if required
#
if (!$log) {
	$log=$(get-location).Path + "\install.log"
}
if (!$err) {
	$err=$(get-location).Path + "\install.err"
}
	
If (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {   

#
# If no Administrative rights, it will display a popup window asking user for Admin rights
#
# Get our script path, set log
#
	$ScriptPath = (Get-Variable MyInvocation).Value.MyCommand.Path

#
# Clean up log files
#
	If (Test-Path $log".err"){
		Remove-Item $log".err" -verbose
	}
	If (Test-Path $log){
		Remove-Item $log -verbose
	}
	If (Test-Path $err){
		Remove-Item $err -verbose
	}
#	
# Build relaunch string
#
	$RelaunchArgs = '-ExecutionPolicy Unrestricted -command "' + $ScriptPath + '" \"' + (get-location).Path + "\" + $installFile + '\" \"' + $installDir + '\"'
	if ($installTree) {
		$RelaunchArgs = $RelaunchArgs + ' \"' + $installTree + '\"'
	}
	else {
		$RelaunchArgs = $RelaunchArgs + ' \"\"'
	}
	$RelaunchArgs = $RelaunchArgs + ' \"' + $log + '\"' + ' \"' + $err + '\"'
	
# DO NOT USE: -NoNewWindow -Wait
	Write-Host "install.ps1 log: $Log"
	Write-Host "install.ps1 arguments: $RelaunchArgs"
	Try {
		$process = Start-Process "$PsHome\PowerShell.exe" -Verb RunAs -ArgumentList $RelaunchArgs -PassThru -Verbose
	}
	Catch {
		Write-Error "install.ps1: ERROR! in Start-Process"
		write-Error "Exception Type: $($_.Exception.GetType().FullName)" 
		write-Error "Exception Message: $($_.Exception.Message)" 
		If (Test-Path $log){
			Write-Host "install.ps1: log: $log >>>"
			get-content -Path $log		
			Write-Host "<<< End of log."
			rename-item -path $log -Newname $log".err" -force -verbose
		}
		exit 2
	}
#
# Wait until the elevated process terminates
#
    while (!($process.HasExited)) {
		Start-Sleep -Seconds 1
    }
#
# End of program
#
	[string]$my_status=$process.ExitCode; # BUG in powershell!
	If (Test-Path $err){
		Write-Host "install.ps1: ERROR! in command execution: $err >>>" -ForegroundColor Red
		$err_msg=get-content -Path $err | Out-String
		Write-Host $err_msg -ForegroundColor Red
		Write-Host "<<< End of error trace." -ForegroundColor Red	
		
		If (Test-Path $log){
			Write-Host "install.ps1: log: $log >>>"
			$log_msg=get-content -Path $log	| Out-String	
			Write-Host $log_msg -ForegroundColor Yellow
			Write-Host "<<< End of log."
			rename-item -path $log -Newname $log".err" -force -verbose
		}
		else {
			Write-Warning "install.ps1: WARNING! no log: $log"	
		}		
		exit 1
	}
	else {
		If (Test-Path $log){
			Write-Host "install.ps1: log: $log >>>"
			$log_msg=get-content -Path $log	| Out-String	
			Write-Host $log_msg -ForegroundColor Yellow	
			Write-Host "<<< End of log."
		}
		else {
			Write-Warning "install.ps1: WARNING! no log: $log"	
		}	
		Write-Host "install.ps1 Command $arguments ran OK."
		exit 0
	}
}
#
# After user clicked Yes on the popup, your file will be reopened with Admin rights
#
function Write-Feedback(){
    param
    (
        [Parameter(Position=0,ValueFromPipeline=$true)]
        [string]$msg,
        [string]$BackgroundColor = "Black",
        [string]$ForegroundColor = "Yellow"
    )

    Write-Host -BackgroundColor $BackgroundColor -ForegroundColor $ForegroundColor $msg;
    $msg | Out-File $log -Append -Width 180;
}
function Write-ErrorFeedback(){
    param
    (
        [Parameter(Position=0,ValueFromPipeline=$true)]
        [string]$msg,
        [string]$BackgroundColor = "Black",
        [string]$ForegroundColor = "Red"
    )

    Write-Host -BackgroundColor $BackgroundColor -ForegroundColor $ForegroundColor $msg;
    $msg | Out-File $err -Append -Width 180;
}

function DoElevatedOperations {
	Write-Feedback "install.ps1 Running PRIVILEGED"
	Write-Feedback "install.ps1 log: $Log"	
	Write-Feedback "install.ps1 errors: $Err" 		
	Write-Feedback "install.ps1 installFile: $installFile" 
	Write-Feedback "install.ps1 installDir: $installDir" 
	
#
# Get filename extension
#
	$extension = [System.IO.Path]::GetExtension($installFile)
	Write-Feedback "install.ps1 installFile extension: $extension" 
	
#
# Clean down unpack tree base directory
#
	If ($installTree) {
		if ($extension -eq ".war") {
			Write-Feedback "install.ps1 installTree: $installTree for WAR files" 
		}
		elseif ($extension -eq ".zip") {
			Throw "install.ps1 installTree: $installTree not valid for ZIP files"
		}
		else {
			Throw "install.ps1 installTree: $installTree not valid for $extension files"		
		}
		
		if (Test-Path $installTree ) {
			Write-Feedback "install.ps1 clean installTree: $installTree" 
			Remove-Item $installTree -recurse -ErrorAction Stop;
		}
		else {
			Write-Feedback "install.ps1 No installTree: $installTree" 
		}
	}
#
# Remove old install war file
#
	If ($installTree) {
		$old_war=$installDir+"\"+([io.fileinfo]$installFile).basename+".war";
		if (Test-Path $old_war) {
			Write-Feedback "install.ps1 clean old install war file: $old_war" 
			Remove-Item $old_war -verbose -ErrorAction Stop;
		}
		else {
			Write-Feedback "install.ps1 No old install war file: $old_war" 
		}
	}
	
#
# Copy file to destination
#	
	If (Test-Path $installDir){ # Destination
		Copy-Item $installFile -Destination $installDir -verbose -ErrorAction Stop
	}
	else {	
		Write-ErrorFeedback "install.ps1: ERROR! Please create directory: $installDir"
		Throw "install.ps1: ERROR! Please create directory: $installDir"
	}
	Write-Feedback "install.ps1: $installFile installed OK."	

#
# Check if WAR expansion directory has been re-created by tomcat
#
	If ($installTree) {
		$i=1;
		for(; $i -le 21; $i++){
			if (Test-Path $installTree) {	
				Write-Feedback "install.ps1: WAR expanded after $i seconds"
				Return;
			}
			Start-Sleep -Seconds 1;
			Write-Feedback "install.ps1: Waiting: $i seconds for WAR expansion"		
		}
		Write-ErrorFeedback "install.ps1: ERROR! WAR not expanded after $i seconds"
		Throw "install.ps1: ERROR! WAR not expanded after $i seconds"
	}
	elseif ($extension -eq ".zip") {
#
# CD to install directory
#
		Set-Location -path $installDir -verbose -ErrorAction Stop

#
# Zip
#
		$7zip = "C:\Program Files\7-Zip\7z.exe"
		if (test-path($7zip)) {
			Write-Feedback "zip.ps1: Using 7zip"
		}
		else {
			Throw "zip.ps1: ERROR! No ZIP program installed; install 7-zip"
		}	
#		$args = "x "+$installFile		
		Write-Feedback "install.ps1: Expanding ZIP file: $installFile in $installDir"	
		$log2=$(get-location).Path + "\install2.log"
#		$process2=(Start-Process $7zip -ArgumentList $args -NoNewWindow -verbose -PassThru -Wait) 2>&1 > $log2
#
# This doesn't capture the output; you get this instead...
#
# Handles  NPM(K)    PM(K)      WS(K) VM(M)   CPU(s)     Id ProcessName
# -------  ------    -----      ----- -----   ------     -- -----------
#      29       5     1312       2864    40     0.02  10388 7z
#	
# And no exit code...	
#

#
# This does work; but us not streamed
#
		& "$7zip" "x" $installFile "-y"	2>&1 > $log2
	
		If (Test-Path $log2){
			Write-Feedback "install.ps1: 7zip log: $log2 >>>"
			$log_msg=get-content -Path $log2	| Out-String	
			Write-Feedback $log_msg -ForegroundColor Yellow	
			Write-Feedback "<<< End of 7zip."
			Remove-Item $log2 -verbose
		}
		Start-Sleep -Seconds 1
	
		if ($LASTEXITCODE -ne 0) {
			Throw "install.ps1: ERROR! $LASTEXITCODE in $7zip $args"
		}

	}
}

#
# Main Try/Catch block
#
Try {
	DoElevatedOperations
}
Catch {
    write-ErrorFeedback "install.ps1: ERROR! in DoElevatedOperations(); Caught an exception:" 
    write-ErrorFeedback "Exception Type: $($_.Exception.GetType().FullName)" 
    write-ErrorFeedback "Exception Message: $($_.Exception.Message)" 
#
	Start-Sleep -Seconds 2
	exit 0
}

#
# Eof