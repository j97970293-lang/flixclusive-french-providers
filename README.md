# Flixclusive French Providers

Extension **Flixclusive** en français pour le catalogue FrenchStream. Le projet est construit à partir du [template officiel Flixclusive](https://github.com/flixclusiveorg/provider-template) et suit le contrat des providers de [flx-providers](https://github.com/flixclusiveorg/flx-providers).

> Cette extension n’héberge, ne stocke et ne distribue aucun contenu. Elle transforme les informations et liens accessibles depuis les sites configurés pour les présenter à Flixclusive. Utilisez-la uniquement avec des contenus et des sources auxquels vous avez légalement accès, et respectez les conditions d’utilisation des services concernés.

## Fonctionnalités

Le provider expose les catalogues de films et de séries, la recherche paginée, la lecture des métadonnées des fiches et la découverte des saisons et épisodes. Les catégories principales incluent les derniers films, les dernières séries, les films populaires, l’action, la comédie, le fantastique, la science-fiction ainsi que plusieurs catalogues de plateformes.

La résolution des lecteurs accepte les URL directes **MP4**, **M3U8**, **VTT** et **SRT** lorsqu’elles sont publiées par la source. Pour les lecteurs intermédiaires, le plugin visite la page du lecteur, recherche les URL média exposées dans les scripts et renvoie les pistes lisibles à Flixclusive. Les versions identifiées comme VF ou VOSTFR sont conservées dans le nom du flux ou de la piste de sous-titres.

La couche réseau utilise une liste de miroirs FrenchStream, une bascule automatique lorsqu’un domaine ne répond pas et des en-têtes de navigateur standards. Aucun secret, aucune clé API et aucun identifiant utilisateur n’est requis.

## Installation depuis un artefact

Compilez le projet avec Gradle, récupérez l’artefact `.flx` produit par le module `FrenchStream`, puis importez-le dans Flixclusive via la gestion des providers personnalisés.

```bash
./gradlew :FrenchStream:make
```

Pour un appareil Android connecté ou un émulateur configuré :

```bash
./gradlew :FrenchStream:deployWithAdb
```

## Développement

Le code est organisé en quatre capacités Flixclusive. `FrenchStreamCatalogApi` gère les catégories et la pagination, `FrenchStreamSearchApi` transforme les cartes HTML en `PartialMedia`, `FrenchStreamMetadataApi` construit les films, séries et épisodes, tandis que `FrenchStreamLinkApi` résout les flux et sous-titres. `FrenchStreamCore` centralise les requêtes HTTP, les miroirs et les parseurs tolérants HTML/JSON.

Les sources de référence étudiées comprennent également les projets français Cloudstream et Nuvio listés dans le brief initial. Les implémentations de lecteurs et de fallback n’ont pas été copiées telles quelles : elles ont servi à identifier des comportements attendus, notamment la priorité aux flux directs, la gestion du `Referer` et la tolérance aux changements de domaine.

## Limites connues

Les sites de streaming peuvent changer leur HTML, leurs domaines, leurs mécanismes anti-robot ou leurs formats de lecteur sans préavis. Un succès de compilation ne garantit donc pas la disponibilité permanente d’un domaine ou d’un flux. Les pages obfusquées qui ne publient aucune URL média exploitable ne sont volontairement pas contournées.

## Licence

Le projet est publié sous licence MIT pour le code original de cette extension. Les marques, logos, métadonnées et contenus accessibles par les sites tiers restent la propriété de leurs ayants droit.
