/** Enregistre un fichier reçu de l'API : un simple lien n'enverrait pas l'en-tête d'authentification. */
export function enregistrerBlob(blob, nom) {
  const url = URL.createObjectURL(blob)
  const lien = document.createElement('a')
  lien.href = url
  lien.download = nom
  lien.click()
  URL.revokeObjectURL(url)
}
