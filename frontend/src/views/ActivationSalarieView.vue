<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { apiPost } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ matricule: '', email: '', motDePasse: '' })
const confirmation = ref('')
const erreur = ref('')
const chargement = ref(false)

async function activer() {
  erreur.value = ''
  if (form.motDePasse !== confirmation.value) {
    erreur.value = 'Les deux mots de passe ne correspondent pas.'
    return
  }
  chargement.value = true
  try {
    await apiPost('/auth/salaries/activation', {
      matricule: form.matricule.trim(),
      email: form.email.trim(),
      motDePasse: form.motDePasse
    })
    try {
      await auth.login(form.email.trim(), form.motDePasse)
      router.push('/salarie/pointage')
    } catch {
      erreur.value = 'Compte activé. Connectez-vous avec votre email et votre mot de passe.'
    }
  } catch (e) {
    erreur.value = e.message || "Impossible d'activer le compte."
  } finally {
    chargement.value = false
  }
}
</script>

<template>
  <div class="activation">
    <form class="box card" @submit.prevent="activer">
      <div class="logo"><AppIcon name="users" :size="24" /></div>
      <h1>Activer mon compte salarié</h1>
      <p class="muted">
        Saisissez le matricule et l'email de votre dossier : le RH vous les a communiqués à votre embauche.
        Vous pourrez ensuite pointer et demander vos congés.
      </p>

      <div class="field">
        <label for="matricule">Matricule</label>
        <input id="matricule" v-model="form.matricule" class="input" placeholder="EMP-2026-0001" required maxlength="50" autocomplete="off" />
      </div>
      <div class="field">
        <label for="email">Email de votre dossier</label>
        <input id="email" v-model="form.email" class="input" type="email" required maxlength="255" autocomplete="email" />
      </div>
      <div class="field">
        <label for="mdp">Mot de passe (8 caractères minimum)</label>
        <input id="mdp" v-model="form.motDePasse" class="input" type="password" required minlength="8" maxlength="72" autocomplete="new-password" />
      </div>
      <div class="field">
        <label for="confirmation">Confirmer le mot de passe</label>
        <input id="confirmation" v-model="confirmation" class="input" type="password" required minlength="8" maxlength="72" autocomplete="new-password" />
      </div>

      <p v-if="erreur" class="erreur" role="alert"><AppIcon name="alert" :size="16" /> {{ erreur }}</p>
      <button class="btn btn-block" :disabled="chargement">{{ chargement ? 'Activation…' : 'Activer mon compte' }}</button>
      <RouterLink class="lien" to="/login">J'ai déjà un compte : me connecter</RouterLink>
    </form>
  </div>
</template>

<style scoped>
.activation { min-height: 100vh; display: grid; place-items: center; padding: 2rem 1rem; background: var(--bg); }
.box { width: 100%; max-width: 440px; padding: 2rem; display: flex; flex-direction: column; gap: 1rem; }
.logo { width: 48px; height: 48px; border-radius: 14px; display: grid; place-items: center; background: var(--primary-50); color: var(--primary); }
h1 { font-size: 1.5rem; font-weight: 800; margin: 0; }
.muted { margin: 0; color: var(--muted); }
.erreur { display: flex; align-items: center; gap: .5rem; color: var(--danger); background: var(--danger-50); padding: .65rem .85rem; border-radius: 10px; font-weight: 500; margin: 0; }
.lien { text-align: center; color: var(--primary); font-weight: 600; }
</style>
