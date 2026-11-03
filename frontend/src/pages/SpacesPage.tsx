import { useEffect, useState } from 'react'
import { ArrowDown, ArrowLeft, ArrowRight, Building2, ChevronDown, CircleCheck, Clock3, DoorOpen, Plus, Search, SlidersHorizontal } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { ApiError } from '../api/http'
import { useAuth } from '../auth/AuthProvider'
import { spaceService, type SpaceFilters } from '../api/spaceService'
import type { Building, PageResult, Space, SpaceStatus, SpaceType } from '../types'

const PAGE_SIZE = 9
const typeLabels: Record<SpaceType, string> = { CLASSROOM: 'Salle de cours', COMPUTER_LAB: 'Laboratoire informatique', MEETING_ROOM: 'Salle de réunion', AMPHITHEATER: 'Amphithéâtre', STUDY_ROOM: 'Salle d’étude', OTHER: 'Autre' }

export function SpacesPage() {
  const { user } = useAuth()
  const location = useLocation()
  const [search, setSearch] = useState('')
  const [type, setType] = useState<SpaceType | ''>('')
  const [buildingId, setBuildingId] = useState('')
  const [status, setStatus] = useState<SpaceStatus | ''>('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<PageResult<Space> | null>(null)
  const [buildings, setBuildings] = useState<Building[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => { void spaceService.buildings().then(setBuildings).catch(() => setBuildings([])) }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    const filters: SpaceFilters = { search, type, buildingId, status, page, size: PAGE_SIZE }
    spaceService.list(filters).then(data => { if (active) setResult(data) })
      .catch(reason => { if (active) setError(reason instanceof ApiError ? reason.message : 'Impossible de charger les espaces.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [search, type, buildingId, status, page, reloadKey])

  function reset() { setSearch(''); setType(''); setBuildingId(''); setStatus(''); setPage(0) }
  const hasFilters = Boolean(search || type || buildingId || status)

  return (
    <div className="page-wrap">
      <div className="breadcrumb"><span>Campus</span><ChevronDown size={13} className="crumb-chevron"/><strong>Espaces</strong></div>
      <section className="page-heading"><div><span className="eyebrow">CATALOGUE DU CAMPUS</span><h1>Espaces</h1><p>Explorez les lieux disponibles pour apprendre, travailler et échanger.</p></div>{user?.role === 'ADMIN' && <Link className="button button-primary" to="/espaces/nouveau"><Plus size={18}/> Nouvel espace</Link>}</section>
      <section className="filter-card" aria-label="Recherche et filtres">
        <label className="search-box"><Search size={18}/><input aria-label="Rechercher par nom ou code" placeholder="Rechercher un nom ou un code…" value={search} onChange={event => { setSearch(event.target.value); setPage(0) }}/>{search && <button className="clear-search" onClick={() => setSearch('')} aria-label="Effacer la recherche">×</button>}</label>
        <div className="filter-controls"><span className="filter-label"><SlidersHorizontal size={15}/> Filtrer par</span>
          <select aria-label="Type d’espace" value={type} onChange={event => { setType(event.target.value as SpaceType | ''); setPage(0) }}><option value="">Tous les types</option>{Object.entries(typeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select aria-label="Bâtiment" value={buildingId} onChange={event => { setBuildingId(event.target.value); setPage(0) }}><option value="">Tous les bâtiments</option>{buildings.map(building => <option key={building.id} value={building.id}>{building.nom}</option>)}</select>
          <select aria-label="Statut" value={status} onChange={event => { setStatus(event.target.value as SpaceStatus | ''); setPage(0) }}><option value="">Tous les statuts</option><option value="ACTIVE">Actif</option><option value="MAINTENANCE">Maintenance</option></select>
          {hasFilters && <button className="text-button" onClick={reset}>Réinitialiser</button>}
        </div>
      </section>
      {(location.state as { notice?: string } | null)?.notice && <div className="form-success page-notice" role="status">{(location.state as { notice: string }).notice}</div>}
      <div className="results-heading"><div><strong>{result?.totalElements ?? '—'}</strong> espace{result?.totalElements === 1 ? '' : 's'}<span className="results-context">{hasFilters ? ' correspondant à votre recherche' : ' dans le catalogue'}</span></div><span className="sort-note"><ArrowDown size={14}/> Nom A–Z</span></div>
      {loading && <div className="state-card" role="status"><span className="spinner"/>Chargement des espaces…</div>}
      {!loading && error && <div className="state-card state-error" role="alert"><strong>Le catalogue est indisponible</strong><span>{error}</span><button className="button button-secondary" onClick={() => setReloadKey(current => current + 1)}>Réessayer</button></div>}
      {!loading && !error && result?.content.length === 0 && <div className="state-card empty-state"><span className="empty-icon"><DoorOpen size={24}/></span><strong>{hasFilters ? 'Aucun résultat pour ces filtres' : 'Aucun espace dans le catalogue'}</strong><span>{hasFilters ? 'Essayez d’élargir votre recherche ou de réinitialiser les filtres.' : 'Les espaces apparaîtront ici lorsqu’ils auront été enregistrés.'}</span>{hasFilters && <button className="button button-secondary" onClick={reset}>Réinitialiser les filtres</button>}</div>}
      {!loading && !error && Boolean(result?.content.length) && <>
        <div className="space-grid">{result?.content.map(space => <article className="space-card" key={space.id}>
          <div className="space-card-top"><div className={`space-icon ${space.type.toLowerCase()}`}><Building2 size={20}/></div><span className={`status-pill ${space.statut === 'ACTIVE' ? 'status-active' : 'status-maintenance'}`}>{space.statut === 'ACTIVE' ? <CircleCheck size={13}/> : <Clock3 size={13}/>} {space.statut === 'ACTIVE' ? 'Actif' : 'Maintenance'}</span></div>
          <div className="space-code">{space.code}</div><h2>{space.nom}</h2><p className="space-type">{typeLabels[space.type]}</p>
          <div className="space-meta"><span><Building2 size={15}/>{space.batiment.nom}{space.etage !== null ? ` · Étage ${space.etage}` : ''}</span><span><DoorOpen size={15}/>{space.capacite} places</span></div>
          <div className="space-card-bottom"><span className={space.reservable ? 'availability available' : 'availability unavailable'}><i/>{space.reservable ? 'Réservable' : 'Non réservable'}</span>{user?.role === 'ADMIN' && <Link className="card-link" to={`/espaces/${space.id}/modifier`}>Modifier <ArrowRight size={15}/></Link>}</div>
        </article>)}</div>
        {result && result.totalPages > 1 && <nav className="pagination" aria-label="Pagination"><button className="button button-secondary page-button" disabled={result.first} onClick={() => setPage(page - 1)}><ArrowLeft size={16}/> Précédent</button><span>Page <strong>{result.number + 1}</strong> sur {result.totalPages}</span><button className="button button-secondary page-button" disabled={result.last} onClick={() => setPage(page + 1)}>Suivant <ArrowRight size={16}/></button></nav>}
      </>}
      <footer className="page-footer"><span>SmartCampus · Catalogue des espaces</span><span>Données mises à jour en temps réel</span></footer>
    </div>
  )
}
