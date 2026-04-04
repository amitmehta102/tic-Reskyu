$srcPath = "c:\Users\Akash\AndroidStudioProjects\tic-Reskyu\ReskyuConsumer\app\src\main\java\com\reskyu\consumer"
$files = Get-ChildItem -Path $srcPath -Recurse -Filter "*.kt"

foreach ($file in $files) {
    $raw = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
    $raw = [Regex]::Replace($raw, '/\*[\s\S]*?\*/', '')
    $lines = $raw -split "`r?`n"
    $kept = foreach ($line in $lines) {
        if ($line -notmatch '^\s*//') { $line }
    }
    $result = [System.Collections.Generic.List[string]]::new()
    $prevBlank = $false
    foreach ($line in $kept) {
        $blank = $line -match '^\s*$'
        if ($blank -and $prevBlank) { continue }
        $result.Add($line)
        $prevBlank = $blank
    }
    while ($result.Count -gt 0 -and $result[$result.Count - 1] -match '^\s*$') {
        $result.RemoveAt($result.Count - 1)
    }
    $newContent = ($result -join "`n") + "`n"
    [System.IO.File]::WriteAllText($file.FullName, $newContent, [System.Text.Encoding]::UTF8)
    Write-Host "Cleaned: $($file.Name) ($($result.Count) lines)"
}
Write-Host "Done."
