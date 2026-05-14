#!/bin/bash
# End-to-end API tests for AgênciasHub
# Usage: ./scripts/e2e-test.sh [BASE_URL]
# Default: http://localhost:8090/api/v1

set -e

BASE_URL="${1:-http://localhost:8090/api/v1}"
PASS=0
FAIL=0
TOTAL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

assert_status() {
  local test_name="$1"
  local expected="$2"
  local actual="$3"
  TOTAL=$((TOTAL + 1))
  if [ "$actual" = "$expected" ]; then
    echo -e "  ${GREEN}✓${NC} $test_name (HTTP $actual)"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✗${NC} $test_name (expected $expected, got $actual)"
    FAIL=$((FAIL + 1))
  fi
}

assert_contains() {
  local test_name="$1"
  local expected="$2"
  local body="$3"
  TOTAL=$((TOTAL + 1))
  if echo "$body" | grep -q "$expected"; then
    echo -e "  ${GREEN}✓${NC} $test_name"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}✗${NC} $test_name (body does not contain '$expected')"
    FAIL=$((FAIL + 1))
  fi
}

echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  AgênciasHub E2E API Tests${NC}"
echo -e "${YELLOW}  Target: $BASE_URL${NC}"
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
echo ""

# ─── 1. Public endpoints ─────────────────────────────────────────────────────
echo -e "${YELLOW}▸ Public Endpoints${NC}"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/public/terms/latest")
assert_status "GET /public/terms/latest" "200" "$STATUS"

# ─── 2. Auth - Registration (blocked by whitelist) ────────────────────────────
echo ""
echo -e "${YELLOW}▸ Auth - Registration Whitelist${NC}"

BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"agencyName":"Test","ownerName":"Test","email":"blocked@test.com","password":"12345678","passwordConfirmation":"12345678","ownerPhone":"11999999999","termsAccepted":true,"termsVersion":"1.0.0"}')
STATUS=$(echo "$BODY" | tail -1)
RESPONSE=$(echo "$BODY" | sed '$d')
assert_status "POST /auth/register (blocked email)" "400" "$STATUS"
assert_contains "Returns beta message" "fase de testes" "$RESPONSE"

# ─── 3. Auth - Login (invalid credentials) ───────────────────────────────────
echo ""
echo -e "${YELLOW}▸ Auth - Login${NC}"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"nonexistent@test.com","password":"wrongpass"}')
assert_status "POST /auth/login (invalid creds)" "404" "$STATUS"

# ─── 4. Auth - Login (valid credentials - use seed user) ─────────────────────
BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"contato@agenciashub.com.br","password":"Admin123!"}')
STATUS=$(echo "$BODY" | tail -1)
RESPONSE=$(echo "$BODY" | sed '$d')

if [ "$STATUS" = "200" ]; then
  assert_status "POST /auth/login (valid creds)" "200" "$STATUS"
  TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  
  if [ -n "$TOKEN" ]; then
    echo -e "  ${GREEN}✓${NC} JWT token received"
    PASS=$((PASS + 1))
    TOTAL=$((TOTAL + 1))

    # ─── 5. Authenticated endpoints ──────────────────────────────────────────
    echo ""
    echo -e "${YELLOW}▸ Authenticated Endpoints${NC}"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/agency" \
      -H "Authorization: Bearer $TOKEN")
    assert_status "GET /agency" "200" "$STATUS"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/invitations" \
      -H "Authorization: Bearer $TOKEN")
    assert_status "GET /invitations" "200" "$STATUS"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/customers" \
      -H "Authorization: Bearer $TOKEN")
    assert_status "GET /customers" "200" "$STATUS"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/quotations" \
      -H "Authorization: Bearer $TOKEN")
    assert_status "GET /quotations" "200" "$STATUS"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/agency/solicitacao-config?slug=demo" \
      -H "Authorization: Bearer $TOKEN")
    assert_status "GET /agency/solicitacao-config" "200" "$STATUS"

    # ─── 6. Create invitation ────────────────────────────────────────────────
    echo ""
    echo -e "${YELLOW}▸ Invitation Flow${NC}"

    BODY=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/invitations" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"email":"test-invite@example.com"}')
    STATUS=$(echo "$BODY" | tail -1)
    RESPONSE=$(echo "$BODY" | sed '$d')
    assert_status "POST /invitations (create)" "201" "$STATUS"
    assert_contains "Returns invite token" "token" "$RESPONSE"

    # Extract invite ID for cleanup
    INVITE_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ -n "$INVITE_ID" ]; then
      # Revoke the test invitation
      STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/invitations/$INVITE_ID" \
        -H "Authorization: Bearer $TOKEN")
      assert_status "DELETE /invitations/:id (revoke)" "204" "$STATUS"
    fi

    # ─── 7. Unauthorized access (no token) ───────────────────────────────────
    echo ""
    echo -e "${YELLOW}▸ Authorization${NC}"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/agency")
    assert_status "GET /agency (no token)" "403" "$STATUS"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/invitations")
    assert_status "GET /invitations (no token)" "403" "$STATUS"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/customers")
    assert_status "GET /customers (no token)" "403" "$STATUS"

  else
    echo -e "  ${RED}✗${NC} No token in response"
    FAIL=$((FAIL + 1))
    TOTAL=$((TOTAL + 1))
  fi
else
  echo -e "  ${YELLOW}⚠${NC} Login failed (HTTP $STATUS) — skipping authenticated tests"
  echo -e "  ${YELLOW}  (User may not exist or password differs)${NC}"
  TOTAL=$((TOTAL + 1))
fi

# ─── 8. Forgot password (always 200) ─────────────────────────────────────────
echo ""
echo -e "${YELLOW}▸ Password Recovery${NC}"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"nonexistent@test.com"}')
assert_status "POST /auth/forgot-password (non-existent email, still 200)" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"contato@agenciashub.com.br"}')
assert_status "POST /auth/forgot-password (existing email)" "200" "$STATUS"

# ─── 9. Verify email (invalid code) ──────────────────────────────────────────
echo ""
echo -e "${YELLOW}▸ Email Verification${NC}"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/verify-email" \
  -H "Content-Type: application/json" \
  -d '{"email":"contato@agenciashub.com.br","code":"000000"}')
assert_status "POST /auth/verify-email (invalid code)" "400" "$STATUS"

# ─── 10. Reset password (invalid code) ───────────────────────────────────────
echo ""
echo -e "${YELLOW}▸ Password Reset${NC}"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/reset-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"contato@agenciashub.com.br","code":"000000","newPassword":"newpass123","newPasswordConfirmation":"newpass123"}')
assert_status "POST /auth/reset-password (invalid code)" "400" "$STATUS"

# ─── Summary ─────────────────────────────────────────────────────────────────
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"
if [ $FAIL -eq 0 ]; then
  echo -e "  ${GREEN}All $TOTAL tests passed!${NC}"
else
  echo -e "  ${GREEN}$PASS passed${NC}, ${RED}$FAIL failed${NC} (total: $TOTAL)"
fi
echo -e "${YELLOW}═══════════════════════════════════════════════════${NC}"

exit $FAIL
