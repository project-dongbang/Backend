param(
    [uri]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

$checks = @(
    @{ Path = '/api/health'; Expected = 200 },
    @{ Path = '/actuator/health/liveness'; Expected = 200 },
    @{ Path = '/actuator/health/readiness'; Expected = 200 },
    @{ Path = '/api/members'; Expected = 401 },
    @{ Path = '/actuator/env'; Expected = 401 }
)

foreach ($check in $checks) {
    $requestUri = [uri]::new($BaseUrl, $check.Path)
    try {
        $response = Invoke-WebRequest -Uri $requestUri -UseBasicParsing -TimeoutSec 10
        $statusCode = [int]$response.StatusCode
    }
    catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }
        $statusCode = [int]$_.Exception.Response.StatusCode
    }

    if ($statusCode -ne $check.Expected) {
        throw "$requestUri returned HTTP $statusCode; expected $($check.Expected)."
    }
    Write-Output "PASS $($check.Path) -> HTTP $statusCode"
}
