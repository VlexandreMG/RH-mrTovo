<template>
  <section class="register">
    <RouterLink class="back" to="/candidats/offres">← Retour aux offres</RouterLink>
    <form class="card" @submit.prevent="inscrire">
      <h1>Créer mon compte candidat</h1>
      <p>Utilisez votre adresse e-mail de candidature pour retrouver les propositions qui vous sont destinées.</p>
      <div class="grid">
        <label>Prénom<input v-model.trim="form.prenom" autocomplete="given-name" required maxlength="255" /></label>
        <label>Nom<input v-model.trim="form.nom" autocomplete="family-name" required maxlength="255" /></label>
      </div>
      <label>E-mail<input v-model.trim="form.email" type="email" autocomplete="email" required maxlength="255" /></label>
      <label>Téléphone (facultatif)<input v-model.trim="form.telephone" type="tel" autocomplete="tel" maxlength="50" /></label>
      <label>Mot de passe<input v-model="form.motDePasse" type="password" autocomplete="new-password" required minlength="8" maxlength="72" /></label>
      <label>Confirmer le mot de passe<input v-model="confirmation" type="password" autocomplete="new-password" required minlength="8" maxlength="72" /></label>
      <p class="notice">Prototype : l’adresse e-mail n’est pas vérifiée. N’utilisez que votre propre adresse.</p>
      <p v-if="erreur" class="error">{{ erreur }}</p>
      <button class="primary" type="submit" :disabled="chargement">
        {{ chargement ? 'Création du compte…' : 'Créer mon compte' }}
      </button>
      <RouterLink class="login" to="/login">J’ai déjà un compte</RouterLink>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiPost } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const chargement = ref(false)
const erreur = ref('')
const confirmation = ref('')
const form = reactive({
  nom: '',
  prenom: '',
  email: '',
  telephone: '',
  motDePasse: ''
})

async function inscrire() {
  erreur.value = ''
  if (form.motDePasse !== confirmation.value) {
    erreur.value = 'Les mots de passe ne correspondent pas.'
    return
  }

  chargement.value = true
  try {
    await apiPost('/auth/candidats/inscription', form)
    try {
      await auth.login(form.email, form.motDePasse)
      router.push('/candidats/espace')
    } catch {
      erreur.value = 'Compte créé. Connectez-vous avec votre adresse e-mail et votre mot de passe.'
    }
  } catch (e) {
    erreur.value = e.message || 'Impossible de créer le compte.'
  } finally {
    chargement.value = false
  }
}
</script>

<style scoped>
.register { max-width: 620px; margin: 0 auto; }
.back { display: inline-block; margin-bottom: 1rem; color: #4f4fff; }
.card { display: flex; flex-direction: column; gap: 1rem; padding: 2rem; border-radius: 18px; background: #fff; box-shadow: 0 40px 70px -50px rgba(60,60,140,.55); }
h1 { margin: 0; color: #14143a; }
.card > p { margin: 0; color: rgba(20,20,58,.72); line-height: 1.5; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
label { display: flex; flex-direction: column; gap: .45rem; color: #14143a; font-weight: 600; }
input { min-width: 0; padding: .75rem .9rem; border: 1px solid rgba(20,20,58,.18); border-radius: 10px; font: inherit; }
.notice { padding: .8rem; border-radius: 10px; background: #fff7e6; color: #795500 !important; font-size: .9rem; }
.error { color: #b42318 !important; }
button.primary { align-self: flex-start; padding: .8rem 1.4rem; border: 0; border-radius: 999px; background: linear-gradient(135deg, #6c6cff, #a05cf0); color: #fff; font: inherit; font-weight: 700; cursor: pointer; }
button:disabled { opacity: .65; cursor: wait; }
.login { text-align: center; color: #4f4fff; font-weight: 600; }
@media (max-width: 620px) { .grid { grid-template-columns: 1fr; } .card { padding: 1.3rem; } }
</style>
