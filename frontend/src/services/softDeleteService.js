import api from './api'

export const softDeleteService = {
  remove(resource, id) {
    return api.delete(`/${resource}/${id}/soft-delete`)
  }
}

export default softDeleteService
