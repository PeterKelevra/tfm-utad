package org.utad.tfm.ppena.core

case class ElasticSearchClusterConfig(val name: String, val uri: String)

case class RsvpEvent(event_id: String, event_name: String, event_url: String, time: Long)

case class RsvpGroup(group_city: String, group_country: String, group_id: Int, group_lat: Float, group_lon: Float,
                     group_name: String, group_state: String, group_urlname: String, group_topics: List[RsvpGroupTopic])

case class RsvpGroupTopic(topic_name: String, urlkey: String)

case class RsvpIdentifier(identifier: String)

case class RsvpMember(member_id: Int, member_name: String, photo: String, other_services: Map[String, RsvpIdentifier])

case class RsvpVenue(venue_id: Int, venue_name: String, lat: Float, lon: Float)

case class Location(lat: Float, lon: Float)

case class Rsvp(rsvp_id: Int, guests: Int, mtime: Long, response: String, visibility: String, var location: Location,
                event: RsvpEvent, member: RsvpMember, group: RsvpGroup, venue: RsvpVenue, var group_members: Int,
                var localized_country_name: String, var local_date: Long, var local_hour: Int, var week_day: String)


case class Category(id: Int, name: String, shortname: String, sort_name: String)

case class Photo(id: String, name: String, highres_link: String, photo_link: String, thumb_link: String)

case class Organizer(id: Int, name: String, bio: String, photo: Photo)

case class Group(id: Int, name: String, link: String, urlname: String, description: String, created: Long, city: String, country: String,
                 localized_country_name: String, state: String, join_mode: String, visibility: String, lat: Float, lon: Float, members: Int,
                 organizer: Organizer, who: String, timezone: String, category: Category, group_photo: Photo, var group_location: Location)


case class EventVenue(id: Int, name: String, city: String, country: String, localized_country_name: String, state: String,
                      address_1: String, lat: Float, lon: Float)


case class Event(var id: String, name: String, utc_offset: Int, time: Long, waitlist_count: Int, yes_rsvp_count: Int, link: String,
                 description: String, visibility: String, var month: String, var week_day: String, var local_time: String, var local_hour: Integer = null,
                 var prediction_yes: Integer = null, var venue: EventVenue, var group: Group)

case class EventCounter(event: RsvpEvent, count: Int)
