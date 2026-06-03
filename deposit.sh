#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Uso: ./deposit.sh <numero_de_cuenta> <monto>"
    exit 1
fi

ACCOUNT_NUMBER=$1
AMOUNT=$2

curl -X POST http://localhost:8081/api/accounts/deposit \
     -H "Content-Type: application/json" \
     -d "{\"accountNumber\": \"$ACCOUNT_NUMBER\", \"amount\": $AMOUNT}"

echo -e "\nDepósito de \$$AMOUNT enviado a la cuenta $ACCOUNT_NUMBER"
