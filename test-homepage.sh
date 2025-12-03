#!/bin/bash

# Test Script for Homepage Implementation
# This script helps test the collaborative text editor homepage feature

echo "========================================="
echo "Collaborative Text Editor - Test Script"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if frontend is running
echo -e "${YELLOW}Checking Frontend...${NC}"
if curl -s http://localhost:4200 > /dev/null; then
    echo -e "${GREEN}✓ Frontend is running on http://localhost:4200${NC}"
else
    echo -e "${RED}✗ Frontend is not running${NC}"
    echo "  Start with: cd editor-client && npm start"
fi
echo ""

# Check if backend is running
echo -e "${YELLOW}Checking Backend...${NC}"
if curl -s http://localhost:8080/api/loadbalancer/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Backend is running on http://localhost:8080${NC}"
else
    echo -e "${RED}✗ Backend is not running${NC}"
    echo "  Start with: cd editor-server && docker compose up -d"
fi
echo ""

# Test document creation API
echo -e "${YELLOW}Testing Document Creation API...${NC}"
if curl -s http://localhost:8080/api/documents/create > /dev/null 2>&1; then
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/documents/create)
    echo -e "${GREEN}✓ Document creation API is working${NC}"
    echo "  Response: $RESPONSE"
else
    echo -e "${RED}✗ Document creation API is not accessible${NC}"
    echo "  Make sure backend is running"
fi
echo ""

# Instructions
echo "========================================="
echo "Manual Testing Steps:"
echo "========================================="
echo ""
echo "1. Open browser: http://localhost:4200"
echo "2. You should see the homepage with 'Blank Document' option"
echo "3. Click on 'Blank Document'"
echo "4. You should be redirected to /editor/doc-{unique-id}"
echo "5. Copy the URL and open in another browser/incognito window"
echo "6. Both windows should show the same document"
echo "7. Type in one window and verify it appears in the other"
echo ""
echo "========================================="
echo "API Testing:"
echo "========================================="
echo ""
echo "Create a new document:"
echo "  curl -X POST http://localhost:8080/api/documents/create"
echo ""
echo "Check if document exists:"
echo "  curl http://localhost:8080/api/documents/{doc-id}/exists"
echo ""
echo "Get document metadata:"
echo "  curl http://localhost:8080/api/documents/{doc-id}"
echo ""
echo "List all documents:"
echo "  curl http://localhost:8080/api/documents"
echo ""
