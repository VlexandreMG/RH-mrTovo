curl -i -X POST http://localhost:8080/api/typeContrat \
  -H "Content-Type: application/json" \
  -d '{"libelle":"CDI"}'

curl -i http://localhost:8080/api/typeContrat

curl -i http://localhost:8080/api/typeContrat/1

curl -i -X PUT http://localhost:8080/api/typeContrat/1 \
  -H "Content-Type: application/json" \
  -d '{"libelle":"CDD"}'

curl -i -X DELETE http://localhost:8080/api/typeContrat/1

curl -i -X POST http://localhost:8080/api/statutContrat \
  -H "Content-Type: application/json" \
  -d '{"libelle":"En cours"}'

curl -i http://localhost:8080/api/employee

curl -i http://localhost:8080/api/statutContrat/1

curl -i -X DELETE http://localhost:8080/api/statutContrat/1

curl -i -X PUT http://localhost:8080/api/employee/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nom":"Rakoto",
    "prenom":"Jean Modifie",
    "email":"jean.rakoto@example.com",
    "telephone":"0347654321",
    "adresse":"Fianarantsoa"
  }'

curl -i -X POST http://localhost:8080/api/contrat \
  -H "Content-Type: application/json" \
  -d '{
    "dateDebut":"2026-11-01",
    "dateFin":"2027-11-01",
    "salaire":250000,
    "typeContrat":{"id":2},
    "statutContrat":{"id":2},
    "employe":{"id":1}
  }'

curl -i http://localhost:8080/api/contrat