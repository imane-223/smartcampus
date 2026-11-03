import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, Save } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/http'
import { spaceService } from '../api/spaceService'
import type { Building, Space, SpacePayload, SpaceStatus, SpaceType } from '../types'

const types: [SpaceType, string][] = [['CLASSROOM', 'Salle de cours'], ['COMPUTER_LAB', 'Laboratoire informatique'], ['MEETING_ROOM', 'Salle de réunion'], ['AMPHITHEATER', 'Amphithéâtre'], ['STUDY_ROOM', 'Salle d’étude'], ['OTHER', 'Autre']]

export function SpaceFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const [buildings, setBuildings] = useState<Building[]>([])
  const [loading, setLoading] = useState(editing)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [success, setSuccess] = useState('')
  const [addingBuilding, setAddingBuilding] = useState(false)
  const [newBuildingName, setNewBuildingName] = useState('')
  const [buildingError, setBuildingError] = useState('')
  const [form, setForm] = useState({ code: '', nom: '', type: 'CLASSROOM' as SpaceType, capacite: '', batimentId: '', etage: '', description: '', statut: 'ACTIVE' as SpaceStatus, archive: false })

  useEffect(() => {
    let active = true
    void Promise.all([spaceService.buildings(), editing ? spaceService.get(Number(id)) : Promise.resolve(null)])
      .then(([buildingList, space]) => {
        if (!active) return
        setBuildings(buildingList)
        if (space) setForm(fromSpace(space))
      })
      .catch(reason => { if (active) setError(reason instanceof ApiError ? reason.message : 'Impossible de charger le formulaire.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [editing, id])

  function set<K extends keyof typeof form>(key: K, value: (typeof form)[K]) { setForm(current => ({ ...current, [key]: value })) }

  async function addBuilding() {
    setBuildingError('')
    if (!newBuildingName.trim()) { setBuildingError('Saisissez le nom du bâtiment.'); return }
    try {
      const building = await spaceService.createBuilding(newBuildingName.trim())
      setBuildings(current => [...current, building])
      set('batimentId', String(building.id))
      setNewBuildingName('')
      setAddingBuilding(false)
    } catch (reason) {
      setBuildingError(reason instanceof ApiError ? reason.message : 'Impossible d’ajouter le bâtiment.')
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(''); setFields({}); setSuccess('')
    const capacity = Number(form.capacite)
    if (!Number.isInteger(capacity) || capacity <= 0) { setFields({ capacite: 'La capacité doit être un entier strictement positif.' }); return }
    if (!form.batimentId) { setFields({ batimentId: 'Choisissez un bâtiment.' }); return }
    const payload: SpacePayload = { code: form.code.trim(), nom: form.nom.trim(), type: form.type, capacite: capacity, batimentId: Number(form.batimentId), etage: form.etage === '' ? null : Number(form.etage), description: form.description.trim() || null, statut: form.statut, ...(editing ? { archive: form.archive } : {}) }
    setBusy(true)
    try {
      const saved = editing ? await spaceService.update(Number(id), payload) : await spaceService.create(payload)
      setSuccess(editing ? 'Les modifications ont été enregistrées.' : 'L’espace a été créé.')
      navigate('/espaces', { replace: true, state: { notice: editing ? 'Espace modifié avec succès.' : 'Espace créé avec succès.', space: saved } })
    } catch (reason) {
      if (reason instanceof ApiError) { setError(reason.message); setFields(reason.fields ?? {}) }
      else setError('L’opération a échoué. Réessayez.')
    } finally { setBusy(false) }
  }

  if (loading) return <div className="page-wrap"><div className="state-card" role="status">Chargement du formulaire…</div></div>
  return <div className="page-wrap form-page">
    <Link className="back-link" to="/espaces"><ArrowLeft size={16}/> Retour au catalogue</Link>
    <div className="form-heading"><span className="eyebrow">ADMINISTRATION DES ESPACES</span><h1>{editing ? 'Modifier un espace' : 'Créer un espace'}</h1><p>{editing ? 'Mettez à jour les informations du lieu.' : 'Ajoutez un nouvel espace au catalogue du campus.'}</p></div>
    <form className="space-form" onSubmit={submit} noValidate>
      {error && <div className="form-error" role="alert">{error}</div>}{success && <div className="form-success" role="status">{success}</div>}
      <div className="form-section"><h2>Informations générales</h2><div className="form-grid">
        <label>Code de l’espace <span className="required">*</span><input required value={form.code} onChange={event => set('code', event.target.value)} placeholder="Ex. SAL-A101" aria-invalid={Boolean(fields.code)}/>{fields.code && <small className="field-error">{fields.code}</small>}</label>
        <label>Nom de l’espace <span className="required">*</span><input required value={form.nom} onChange={event => set('nom', event.target.value)} placeholder="Ex. Salle A101" aria-invalid={Boolean(fields.nom)}/>{fields.nom && <small className="field-error">{fields.nom}</small>}</label>
        <label>Type <span className="required">*</span><select value={form.type} onChange={event => set('type', event.target.value as SpaceType)}>{types.map(([value, text]) => <option key={value} value={value}>{text}</option>)}</select>{fields.type && <small className="field-error">{fields.type}</small>}</label>
        <label>Capacité (places) <span className="required">*</span><input required type="number" min="1" step="1" value={form.capacite} onChange={event => set('capacite', event.target.value)} placeholder="40" aria-invalid={Boolean(fields.capacite)}/>{fields.capacite && <small className="field-error">{fields.capacite}</small>}</label>
        <div className="building-field"><label>Bâtiment <span className="required">*</span><select required value={form.batimentId} onChange={event => set('batimentId', event.target.value)}><option value="">Sélectionner un bâtiment</option>{buildings.map(building => <option key={building.id} value={building.id}>{building.nom}</option>)}</select>{fields.batimentId && <small className="field-error">{fields.batimentId}</small>}</label>
          <button type="button" className="text-button building-add-toggle" onClick={() => { setAddingBuilding(current => !current); setBuildingError('') }}>{addingBuilding ? 'Annuler' : '+ Ajouter un bâtiment'}</button>
          {addingBuilding && <div className="building-add"><input aria-label="Nom du nouveau bâtiment" value={newBuildingName} onChange={event => setNewBuildingName(event.target.value)} maxLength={150} placeholder="Nom du bâtiment"/><button type="button" className="button button-secondary" onClick={() => void addBuilding()}>Ajouter</button></div>}
          {buildingError && <small className="field-error building-error">{buildingError}</small>}
        </div>
        <label>Étage <span className="optional">Facultatif</span><input type="number" step="1" value={form.etage} onChange={event => set('etage', event.target.value)} placeholder="Ex. 1"/></label>
        <label className="span-two">Description <span className="optional">Facultative</span><textarea rows={3} maxLength={2000} value={form.description} onChange={event => set('description', event.target.value)} placeholder="Précisions utiles sur cet espace…"/></label>
        <label>Statut opérationnel<select value={form.statut} onChange={event => set('statut', event.target.value as SpaceStatus)}><option value="ACTIVE">Actif</option><option value="MAINTENANCE">En maintenance</option></select></label>
        {editing && <label className="checkbox-row"><input type="checkbox" checked={form.archive} onChange={event => set('archive', event.target.checked)}/> Archiver cet espace</label>}
      </div></div>
      <div className="form-actions"><Link className="button button-secondary" to="/espaces">Annuler</Link><button className="button button-primary" type="submit" disabled={busy || buildings.length === 0}><Save size={17}/>{busy ? 'Enregistrement…' : editing ? 'Enregistrer les modifications' : 'Créer l’espace'}</button></div>
    </form>
  </div>
}

function fromSpace(space: Space) {
  return { code: space.code, nom: space.nom, type: space.type, capacite: String(space.capacite), batimentId: String(space.batiment.id), etage: space.etage === null ? '' : String(space.etage), description: space.description ?? '', statut: space.statut, archive: space.archive }
}
