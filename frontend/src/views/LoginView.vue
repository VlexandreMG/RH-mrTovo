<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const motDePasse = ref('')
const erreur = ref('')
const chargement = ref(false)

const comptes = [
  { role: 'Manager Informatique', email: 'manager.informatique@rh.com', mdp: 'manager123', icon: 'briefcase' },
  { role: 'Manager RH', email: 'manager.rh@rh.com', mdp: 'manager123', icon: 'briefcase' },
  { role: 'Manager Finance', email: 'manager.finance@rh.com', mdp: 'manager123', icon: 'briefcase' },
  { role: 'Manager Direction', email: 'manager.direction@rh.com', mdp: 'manager123', icon: 'briefcase' },
  { role: 'Manager Commercial', email: 'manager.commercial@rh.com', mdp: 'manager123', icon: 'briefcase' },
  { role: 'RH', email: 'rh@rh.com', mdp: 'rh123', icon: 'users' },
  { role: 'Finance', email: 'finance@rh.com', mdp: 'finance123', icon: 'wallet' },
  { role: 'Direction', email: 'direction@rh.com', mdp: 'direction123', icon: 'shield' }
]

function remplir(c) {
  email.value = c.email
  motDePasse.value = c.mdp
}

async function connexion() {
  erreur.value = ''
  chargement.value = true
  try {
    await auth.login(email.value.trim(), motDePasse.value)
    router.push('/')
  } catch (e) {
    erreur.value = e.message.includes('serveur') ? e.message : 'Email ou mot de passe incorrect.'
  } finally {
    chargement.value = false
  }
}
</script>

<template>
  <div class="login">
    <section class="hero">
      <div class="hero-inner">
        <div class="logo"><AppIcon name="users" :size="26" /></div>
        <h1>Exprimez, validez et ouvrez vos recrutements.</h1>
        <p>Un seul circuit pour tous : le manager exprime le besoin, le RH chiffre, la Finance donne son avis et la Direction tranche.</p>
        <ul>
          <li><AppIcon name="check" :size="16" /> Effectifs par niveau (junior, confirmé, senior)</li>
          <li><AppIcon name="check" :size="16" /> Coûts estimés à partir de la grille salariale</li>
          <li><AppIcon name="check" :size="16" /> Historique complet de chaque décision</li>
        </ul>
      </div>
      <div class="orb o1" /><div class="orb o2" />
    </section>

    <section class="panel">
      <form class="box" @submit.prevent="connexion">
        <h2>Connexion</h2>
        <p class="muted">Accédez à votre espace de gestion des besoins.</p>

        <div class="field">
          <label for="email">Adresse email</label>
          <input id="email" v-model="email" class="input" type="email" placeholder="prenom@entreprise.com" required autocomplete="username" />
        </div>
        <div class="field">
          <label for="mdp">Mot de passe</label>
          <input id="mdp" v-model="motDePasse" class="input" type="password" placeholder="••••••••" required autocomplete="current-password" />
        </div>

        <p v-if="erreur" class="erreur"><AppIcon name="alert" :size="16" /> {{ erreur }}</p>
        <button class="btn btn-block" :disabled="chargement">{{ chargement ? 'Connexion…' : 'Se connecter' }}</button>

        <div class="demo">
          <span class="label">Comptes de démonstration</span>
          <div class="demo-grid">
            <button v-for="c in comptes" :key="c.email" type="button" class="demo-btn" @click="remplir(c)">
              <AppIcon :name="c.icon" :size="16" /> {{ c.role }}
            </button>
          </div>
          <small class="hint">Cliquez sur un rôle pour pré-remplir le formulaire.</small>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.login { display: grid; grid-template-columns: 1.1fr 1fr; min-height: 100vh; }
.hero { position: relative; overflow: hidden; background: linear-gradient(145deg, #1a1a4e 0%, #2d2a8f 55%, #6a3fd0 100%); color: #fff; display: flex; align-items: center; padding: 3rem; }
.hero-inner { position: relative; z-index: 2; max-width: 480px; }
.logo { width: 54px; height: 54px; border-radius: 16px; display: grid; place-items: center; background: rgba(255, 255, 255, .16); backdrop-filter: blur(6px); margin-bottom: 1.6rem; }
.hero h1 { font-size: 2.2rem; line-height: 1.15; font-weight: 800; margin-bottom: 1rem; }
.hero p { color: #d4d5f5; font-size: 1.02rem; margin-bottom: 1.6rem; }
.hero ul { list-style: none; display: flex; flex-direction: column; gap: .7rem; }
.hero li { display: flex; align-items: center; gap: .6rem; color: #eceeff; }
.hero li svg { background: rgba(255, 255, 255, .2); border-radius: 50%; padding: 3px; width: 22px; height: 22px; }
.orb { position: absolute; border-radius: 50%; filter: blur(2px); }
.o1 { width: 380px; height: 380px; right: -120px; top: -100px; background: radial-gradient(circle, rgba(160, 120, 255, .55), transparent 70%); }
.o2 { width: 460px; height: 460px; left: -160px; bottom: -200px; background: radial-gradient(circle, rgba(90, 120, 255, .5), transparent 70%); }
.panel { display: grid; place-items: center; padding: 2rem; background: var(--bg); }
.box { width: 100%; max-width: 420px; display: flex; flex-direction: column; gap: 1.1rem; }
.box h2 { font-size: 1.7rem; font-weight: 800; }
.erreur { display: flex; align-items: center; gap: .5rem; color: var(--danger); background: var(--danger-50); padding: .65rem .85rem; border-radius: 10px; font-weight: 500; }
.demo { margin-top: .6rem; padding: 1rem; border: 1px dashed #c9cce6; border-radius: 12px; display: flex; flex-direction: column; gap: .7rem; background: rgba(255, 255, 255, .6); }
.demo-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
.demo-btn { display: flex; align-items: center; gap: .5rem; justify-content: center; padding: .55rem; border-radius: 9px; border: 1px solid var(--border); background: #fff; cursor: pointer; font-weight: 600; font-size: .86rem; transition: all .15s; }
.demo-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-50); }
@media (max-width: 900px) { .login { grid-template-columns: 1fr; } .hero { display: none; } }
</style>
