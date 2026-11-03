import { request } from './http'
import type { Building, PageResult, Space, SpacePayload, SpaceStatus, SpaceType } from '../types'

export interface SpaceFilters {
  search: string
  type: SpaceType | ''
  buildingId: string
  status: SpaceStatus | ''
  page: number
  size: number
}

export const spaceService = {
  list(filters: SpaceFilters) {
    const query = new URLSearchParams({ page: String(filters.page), size: String(filters.size), sort: 'nom,asc' })
    if (filters.search.trim()) query.set('search', filters.search.trim())
    if (filters.type) query.set('type', filters.type)
    if (filters.buildingId) query.set('batimentId', filters.buildingId)
    if (filters.status) query.set('statut', filters.status)
    return request<PageResult<Space>>(`/api/salles?${query}`)
  },
  get(id: number) {
    return request<Space>(`/api/salles/${id}`)
  },
  buildings() {
    return request<Building[]>('/api/salles/batiments')
  },
  createBuilding(nom: string) {
    return request<Building>('/api/salles/batiments', { method: 'POST', body: JSON.stringify({ nom }) })
  },
  create(payload: SpacePayload) {
    return request<Space>('/api/salles', { method: 'POST', body: JSON.stringify(payload) })
  },
  update(id: number, payload: SpacePayload) {
    return request<Space>(`/api/salles/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
  },
}
