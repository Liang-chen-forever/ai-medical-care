# Scores agreement with human-labelled synthetic triage cases; it does not claim clinical accuracy.
[CmdletBinding()]
param(
    [string]$DatasetPath = (Join-Path $PSScriptRoot '..\evaluation\triage-cases.jsonl'),
    [string]$PredictionsPath
)

$ErrorActionPreference = 'Stop'
$rows = @(Get-Content -LiteralPath $DatasetPath | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object { $_ | ConvertFrom-Json })
if ($rows.Count -eq 0) { throw 'Evaluation dataset is empty' }
$ids = @($rows | ForEach-Object id)
if (($ids | Sort-Object -Unique).Count -ne $rows.Count) { throw 'Evaluation dataset contains duplicate ids' }

Write-Output ("dataset_rows={0}" -f $rows.Count)
if ([string]::IsNullOrWhiteSpace($PredictionsPath)) {
    Write-Output 'score=not_computed (provide -PredictionsPath; labels are not model metrics)'
    exit 0
}

$predictions = @(Get-Content -LiteralPath $PredictionsPath | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object { $_ | ConvertFrom-Json })
$byId = @{}
foreach ($prediction in $predictions) { $byId[$prediction.id] = $prediction }
$matched = 0
$scored = 0
foreach ($row in $rows) {
    if (-not $byId.ContainsKey($row.id)) { continue }
    $scored++
    $prediction = $byId[$row.id]
    if ($prediction.department -eq $row.expectedDepartment -and $prediction.risk -eq $row.expectedRisk) { $matched++ }
}
if ($scored -eq 0) { throw 'No prediction ids matched the dataset' }
$agreement = [math]::Round($matched / $scored, 4)
Write-Output ("label_agreement={0}; matched={1}; scored={2}" -f $agreement, $matched, $scored)
Write-Output 'note=label agreement is an offline engineering signal, not a medical accuracy or safety claim'
