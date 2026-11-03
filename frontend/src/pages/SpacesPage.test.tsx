import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { SpacesPage } from './SpacesPage'
import type { PageResult, Space } from '../types'

const mocks = vi.hoisted(() => ({ list: vi.fn(), buildings: vi.fn(), user: { role: 'ADMIN', prenom: 'Nadia', nom: 'Idrissi' } }))
vi.mock('../api/spaceService', () => ({ spaceService: { list: mocks.list, buildings: mocks.buildings } }))
vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: mocks.user }) }))

const space: Space = { id: 1, code: 'SAL-A101', nom: 'Salle A101', type: 'CLASSROOM', capacite: 40,
  batiment: { id: 1, nom: 'Bâtiment A' }, etage: 1, description: null, statut: 'ACTIVE', archive: false, reservable: true }
const page = (content: Space[]): PageResult<Space> => ({ content, number: 0, size: 9, totalElements: content.length, totalPages: content.length ? 1 : 0, first: true, last: true })
const renderPage = () => render(<MemoryRouter><SpacesPage/></MemoryRouter>)

describe('SpacesPage', () => {
  beforeEach(() => {
    cleanup()
    vi.clearAllMocks()
    mocks.user.role = 'ADMIN'
    mocks.user.prenom = 'Nadia'
    mocks.user.nom = 'Idrissi'
    mocks.buildings.mockResolvedValue([{ id: 1, nom: 'Bâtiment A' }])
    mocks.list.mockResolvedValue(page([space]))
  })

  it('affiche le catalogue et les actions administrateur', async () => {
    renderPage()
    expect(await screen.findByText('Salle A101')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /nouvel espace/i })).toBeInTheDocument()
    expect(screen.getByText('Réservable')).toBeInTheDocument()
  })

  it('masque les actions administrateur aux étudiants', async () => {
    mocks.user.role = 'STUDENT'
    renderPage()
    await screen.findByText('Salle A101')
    expect(screen.queryByRole('link', { name: /nouvel espace/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /modifier/i })).not.toBeInTheDocument()
  })

  it('transmet la recherche au backend', async () => {
    renderPage()
    await screen.findByText('Salle A101')
    fireEvent.change(screen.getByRole('textbox', { name: /rechercher par nom ou code/i }), { target: { value: 'A101' } })
    await waitFor(() => expect(mocks.list).toHaveBeenLastCalledWith(expect.objectContaining({ search: 'A101', page: 0 })))
  })

  it('transmet les filtres et permet de les réinitialiser', async () => {
    renderPage()
    await screen.findByText('Salle A101')
    fireEvent.change(screen.getByLabelText('Statut'), { target: { value: 'MAINTENANCE' } })
    await waitFor(() => expect(mocks.list).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'MAINTENANCE' })))
    fireEvent.click(screen.getByRole('button', { name: /réinitialiser/i }))
    await waitFor(() => expect(mocks.list).toHaveBeenLastCalledWith(expect.objectContaining({ status: '', type: '', buildingId: '', search: '', page: 0 })))
  })

  it('affiche un état vide', async () => {
    mocks.list.mockResolvedValueOnce(page([]))
    renderPage()
    expect(await screen.findByText('Aucun espace dans le catalogue')).toBeInTheDocument()
  })

  it('affiche les erreurs du backend', async () => {
    mocks.list.mockRejectedValueOnce(new Error('Backend indisponible'))
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger les espaces.')
  })
})
