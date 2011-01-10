package se.raa.ksamsok.api;

import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.organization.OrganizationManager;
import se.raa.ksamsok.solr.SearchService;
import se.raa.ksamsok.statistic.StatisticsManager;

/**
 * Interface f�r att koppla loss factory fr�n metoderna och undvika cirkelberoenden.
 */
public interface APIServiceProvider {

	/**
	 * Ger s�ktj�nst
	 * @return the searchService
	 */
	SearchService getSearchService();

	/**
	 * Ger repository manager
	 * @return the repository manager
	 */
	HarvestRepositoryManager getHarvestRepositoryManager();

	/**
	 * Ger organization manager
	 * @return the organization manager
	 */
	OrganizationManager getOrganizationManager();

	/**
	 * Ger statistics manager
	 * @return the statistics manager
	 */
	StatisticsManager getStatisticsManager();
}
