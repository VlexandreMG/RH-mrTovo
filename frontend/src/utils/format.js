const nombre = new Intl.NumberFormat('fr-FR')

export function ar(valeur) {
  if (valeur === null || valeur === undefined || valeur === '') return '—'
  return nombre.format(Math.round(Number(valeur))) + ' Ar'
}

export function nb(valeur) {
  return nombre.format(Number(valeur) || 0)
}

export function date(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function dateHeure(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('fr-FR', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

export const STATUTS = {
  EN_ATTENTE_RH: { label: 'En attente RH', tone: 'warning' },
  EN_ATTENTE_FINANCE: { label: 'En attente Finance', tone: 'info' },
  EN_ATTENTE_DIRECTION: { label: 'En attente Direction', tone: 'violet' },
  AUTORISEE: { label: 'Autorisée', tone: 'primary' },
  RECRUTEMENT_OUVERT: { label: 'Recrutement ouvert', tone: 'success' },
  REFUSEE: { label: 'Refusée', tone: 'danger' }
}

export const ROLES = {
  MANAGER: 'Manager',
  RH: 'Ressources humaines',
  FINANCE: 'Finance',
  DIRECTION: 'Direction'
}

export const TYPES_BESOIN = {
  CREATION: 'Création',
  REMPLACEMENT: 'Remplacement'
}

/** Statut sur lequel chaque rôle doit agir. */
export const A_TRAITER = {
  RH: ['EN_ATTENTE_RH', 'AUTORISEE'],
  FINANCE: ['EN_ATTENTE_FINANCE'],
  DIRECTION: ['EN_ATTENTE_DIRECTION'],
  MANAGER: []
}
