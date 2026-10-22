<script setup>
import { onMounted, ref } from 'vue';
const typeContrat = ref([])
const statutContrat = ref([])
const employe = ref([])

const idTypeContrat = ref('')
const idStatutContrat = ref('')
const idEmploye = ref('')

const dateDebut = ref('')
const dateFin = ref('')
const salaire = ref('')


const erreur = ref('')
async function chargerListes(){
    try {
        const[typeContratResponse, statutResponse, employeResponse]= await Promise.all([
            fetch('http://localhost:8081/api/typeContrat'),
            fetch('http://localhost:8081/api/statutContrat'),
            fetch('http://localhost:8081/api/employee')
        ]);
        if(!typeContratResponse.ok || !statutResponse.ok || !employeResponse.ok) {
            throw new Error('Erreur lors du chargement des listes');
        }
        typeContrat.value = await typeContratResponse.json();
        statutContrat.value = await statutResponse.json();
        employe.value = await employeResponse.json();
    } catch (error) {
        erreur.value = error.message;
    }

}
async function creerContrat(){
    erreur.value = ''
    if(!idTypeContrat.value || !idStatutContrat.value || !idEmploye.value || !dateDebut.value || !dateFin.value || !salaire.value){
        erreur.value = 'Veuillez remplir tous les champs';
        return;
    }
    const contrat = {
        dateDebut:dateDebut.value,
        dateFin:dateFin.value,
        salaire:Number(salaire.value),
        typeContrat:{id : Number(idTypeContrat.value)},
        statutContrat:{id : Number(idStatutContrat.value)},
        employe:{id : Number(idEmploye.value)}

    }
    try {
        const reponse = await fetch('http://localhost:8081/api/contrat',{
            method: 'POST',
            headers : {'Content-Type':'application/json'},
            body: JSON.stringify(contrat)
        })
        if(reponse.status !== 201){
            throw new Error('Erreur lors de la création du contrat');
        } else {
            alert('Contrat créé avec succès');
        }
    } catch (error) {
        erreur.value = error.message;
    }
}
onMounted(() => {
    chargerListes();
});
</script>
<template>
    <form class="contrat-form" @submit.prevent="creerContrat">
    <div class="form-content">
        <h1>Créer un contrat</h1>
        <p v-if="erreur" class="error">{{ erreur }}</p>

        <div class="field">
        <label for="idTypeContrat">Type de contrat</label>
        <select id="idTypeContrat" v-model="idTypeContrat">
            <option value="">Sélectionnez un type de contrat</option>
            <option v-for="type in typeContrat" :key="type.id" :value="type.id">{{ type.libelle }}</option>
        </select>
        </div>

        <div class="field">
        <label for="idStatutContrat">Statut du contrat</label>
        <select id="idStatutContrat" v-model="idStatutContrat">
            <option value="">Sélectionnez un statut de contrat</option>
            <option v-for="statut in statutContrat" :key="statut.id" :value="statut.id">{{ statut.libelle}}</option>
        </select>
        </div>

        <div class="field">
        <label for="employe">Employé</label>
        <select id="idEmploye" v-model="idEmploye">
            <option value="">Sélectionnez un employé</option>
            <option v-for="emp in employe" :key="emp.id" :value="emp.id">{{ emp.nom }} {{ emp.prenom }}</option>
        </select>
        </div>

        <div class="field">
        <label for="dateDebut">Date de début</label>
        <input type="date" id="dateDebut" v-model="dateDebut" />
        </div>

        <div class="field">
        <label for="dateFin">Date de fin</label>
        <input type="date" id="dateFin" v-model="dateFin" />
        </div>

        <div class="field">
        <label for="salaire">Salaire</label>
        <input type="number" id="salaire" v-model="salaire" />
        </div>

        <button class="submit-button" type="submit">Créer le contrat</button>
    </div>
    </form>
</template>

<style scoped>
.contrat-form {
    width: min(100%, 720px);
    margin: 2rem auto;
    padding: 1.5rem;
    background: #ffffff;
    border: 1px solid #dddddd;
    border-radius: 6px;
}

.form-content {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

h1 {
    grid-column: 1 / -1;
    margin: 0 0 0.5rem;
    font-size: 1.5rem;
    color: #222222;
}

.field {
    display: grid;
    gap: 0.35rem;
}

label {
    font-size: 0.9rem;
    color: #444444;
}

input,
select {
    width: 100%;
    min-height: 2.4rem;
    padding: 0.45rem 0.6rem;
    border: 1px solid #bbbbbb;
    border-radius: 4px;
    background: #ffffff;
    font: inherit;
}

input:focus,
select:focus {
    outline: 2px solid #9ac7b6;
    outline-offset: 1px;
}

.error {
    grid-column: 1 / -1;
    margin: 0;
    color: #b42318;
}

.submit-button {
    grid-column: 1 / -1;
    justify-self: start;
    padding: 0.6rem 1rem;
    border: 0;
    border-radius: 4px;
    background: #2f7d61;
    color: #ffffff;
    font: inherit;
    cursor: pointer;
}

.submit-button:hover {
    background: #25664f;
}

@media (max-width: 600px) {
    .contrat-form {
        margin: 1rem auto;
        padding: 1rem;
    }

    .form-content {
        grid-template-columns: 1fr;
    }
}
</style>
