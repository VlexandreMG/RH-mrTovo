<script setup>
import {ref , onMounted} from 'vue';
import axios from 'axios';

// Liste des candidatures 
const listCandidatures = ref([]);

const fetchCandidatures = async () => {
    try {
        const response = await axios.get('http://localhost:8080/api/candidatures/offre/${offreId}');
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
</template>

<style scoped>
</style>