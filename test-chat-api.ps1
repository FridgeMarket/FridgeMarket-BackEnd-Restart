# PowerShell test script for Chat APIs
# How to use:
# 1) Run backend server (Spring Boot) locally on http://localhost:8080
# 2) Set $TOKEN to a valid Access Token (JWT)
# 3) Set $RECEIVER_NICK to target user's nickname
# 4) Optionally set $CHAT_NUM to an existing message ID for check/reply/delete tests
# 5) Execute functions at the bottom (uncomment the examples)

$BASE_URL = "http://localhost:8080"
$TOKEN = "PASTE_ACCESS_TOKEN_HERE"
$RECEIVER_NICK = "상대닉네임_여기에"
$CHAT_NUM = 123

$Headers = @{
  "Content-Type" = "application/json"
  "Authorization" = "Bearer $TOKEN"
}

function Send-Chat {
  param(
    [Parameter(Mandatory=$true)][string]$ReceiverNickname,
    [Parameter(Mandatory=$true)][string]$Content
  )
  $body = @{ receiverNickname = $ReceiverNickname; content = $Content } | ConvertTo-Json -Depth 3
  Invoke-RestMethod -Method Post -Uri "$BASE_URL/api/chats" -Headers $Headers -Body $body
}

function Get-Inbox {
  Invoke-RestMethod -Method Get -Uri "$BASE_URL/api/chats/receive-message" -Headers $Headers
}

function Get-Sent {
  Invoke-RestMethod -Method Get -Uri "$BASE_URL/api/chats/send-message" -Headers $Headers
}

function Get-Message {
  param([Parameter(Mandatory=$true)][long]$ChatNum)
  Invoke-RestMethod -Method Get -Uri "$BASE_URL/api/chats/check-message/$ChatNum" -Headers $Headers
}

function Reply-Chat {
  param(
    [Parameter(Mandatory=$true)][long]$ChatNum,
    [Parameter(Mandatory=$true)][string]$Content
  )
  $body = @{ content = $Content } | ConvertTo-Json -Depth 3
  Invoke-RestMethod -Method Post -Uri "$BASE_URL/api/chats/reply/$ChatNum" -Headers $Headers -Body $body
}

function Delete-Chat {
  param([Parameter(Mandatory=$true)][long]$ChatNum)
  Invoke-RestMethod -Method Delete -Uri "$BASE_URL/api/chats/delete-message/$ChatNum" -Headers $Headers
}

# =========================
# Example usage (uncomment)
# =========================

# 1) Send a message
# Send-Chat -ReceiverNickname $RECEIVER_NICK -Content "안녕하세요. 거래 가능할까요?"

# 2) Get inbox (received messages)
# Get-Inbox | Format-List

# 3) Get sent messages
# Get-Sent | Format-List

# 4) Get a specific message
# Get-Message -ChatNum $CHAT_NUM | Format-List

# 5) Reply to a message
# Reply-Chat -ChatNum $CHAT_NUM -Content "네, 가능합니다."

# 6) Delete a message
# Delete-Chat -ChatNum $CHAT_NUM


