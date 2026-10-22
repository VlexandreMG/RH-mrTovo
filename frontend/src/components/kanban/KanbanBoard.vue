<script setup>
import {ref , onMounted} from 'vue';
import axios from 'axios';
import { useRoute } from 'vue-router';

// Tous ce qui concerne la page 
const route = useRoute();
const offreId = route.params.id;

// Liste des candidatures 
const listCandidatures = ref([]);

const fetchCandidatures = async () => {
    try {
        const response = await axios.get(`http://localhost:8080/api/candidatures/offre/${offreId}`);
        listCandidatures.value = response.data;    
    } catch (error) {
        console.error("Erreur lors du chargement des candidatures",error);
    }
}

onMounted (() => {
    fetchCandidatures();
});
</script>

<template>
    <h2>Suivi des candidatures - Offre #{{ offreId }}</h2>
    <button>Retour aux offres</button>
</template>

<style scoped>
</style>