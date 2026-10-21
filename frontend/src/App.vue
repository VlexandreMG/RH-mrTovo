<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="b3-shell">
    <aside class="b3-sidebar">
      <div class="b3-brand">
        <strong>MRTOVO · Bloc 3</strong>
        <span>Sélection &amp; embauche</span>
      </div>
      <nav class="b3-nav">
        <RouterLink to="/bloc3/dashboard">Tableau de bord</RouterLink>
        <RouterLink to="/bloc3/candidatures">Candidatures</RouterLink>
        <RouterLink to="/bloc3/propositions">Propositions</RouterLink>
        <RouterLink to="/bloc3/negotiations">Négociations</RouterLink>
        <RouterLink to="/bloc3/employes">Employés</RouterLink>
        <RouterLink to="/bloc3/contrats">Contrats</RouterLink>
      </nav>
      <p class="b3-sidebar-footer">
        Cycle de vie : Candidature (Bloc 2) → Proposition → Négociation → Acceptation → Employé, contrat,
        affectation.
      </p>
    </aside>
    <main class="b3-main">
      <RouterView />
    </main>
  </div>
</template>
