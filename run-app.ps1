# Compile and Run Spring Boot Application with clean dependency resolution
$ErrorActionPreference = "Stop"

$m2Repo = "C:\Users\USER\.m2\repository"
$targetDir = "c:\Users\USER\Downloads\hospital\target\classes"

# 1. Clean and recreate target/classes
if (Test-Path $targetDir) {
    Remove-Item -Path $targetDir -Recurse -Force
}
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

# 2. Collect jars from .m2, excluding legacy SLF4J 1.x and unwanted conflicting versions
Write-Host "Collecting classpath dependencies from .m2 repository..."
$allJars = Get-ChildItem -Path $m2Repo -Filter "*.jar" -Recurse | Where-Object { 
    $_.FullName -notmatch "-sources\.jar" -and 
    $_.FullName -notmatch "-javadoc\.jar" -and
    $_.FullName -notmatch "slf4j-api[\\/]1\." -and
    $_.FullName -notmatch "slf4j-simple" -and
    $_.FullName -notmatch "slf4j-nop" -and
    $_.FullName -notmatch "slf4j-log4j12" -and
    $_.FullName -notmatch "logback-classic[\\/]1\.[1-3]\."
}

# Deduplicate by jar file name base, keeping the latest version
$jarMap = @{}
foreach ($j in $allJars) {
    $name = $j.Name
    if (-not $jarMap.ContainsKey($name)) {
        $jarMap[$name] = $j.FullName
    }
}
$jars = $jarMap.Values

$classpath = ($jars -join ";")

# 3. Collect Java source files
$javaFiles = Get-ChildItem -Path "c:\Users\USER\Downloads\hospital\src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

Write-Host "Compiling $($javaFiles.Count) Java source files with javac..."
& "javac" -d $targetDir -cp $classpath $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
    exit 1
}

Write-Host "Compilation successful!"

# 4. Copy resources to target/classes
Write-Host "Copying application resources and templates..."
Copy-Item -Path "c:\Users\USER\Downloads\hospital\src\main\resources\*" -Destination $targetDir -Recurse -Force

Write-Host "Build complete! Launching HospitalAppointmentApplication on port 8080..."
$fullCp = "$targetDir;$classpath"
& "java" -cp $fullCp com.hospital.appointment.HospitalAppointmentApplication
