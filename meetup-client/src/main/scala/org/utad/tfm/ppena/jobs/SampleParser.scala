package org.utad.tfm.ppena.jobs

import org.utad.tfm.ppena.core.Event
import org.utad.tfm.ppena.core.Group
import org.utad.tfm.ppena.core.Rsvp
import org.utad.tfm.ppena.core.Util
import org.utad.tfm.ppena.meetup.MeetupApi

object SampleParser extends App {

  var jsonRsvp = """{"response":"yes","member":{"member_name":"Esther","photo":"http:\/\/photos1.meetupstatic.com\/photos\/member\/b\/b\/0\/thumb_159962992.jpeg","member_id":120762942},"visibility":"public","event":{"time":1427551200000,"event_url":"http:\/\/www.meetup.com\/ELEOS-Art-School-Studio\/events\/220993343\/","event_id":"220993343","event_name":"Paper Collage"},"guests":0,"mtime":1425788568167,"rsvp_id":1540021212,"group":{"group_name":"ELEOS Art School & Studio","group_state":"NC","group_city":"Garner","group_lat":35.68,"group_urlname":"ELEOS-Art-School-Studio","group_id":13434212,"group_country":"us","group_topics":[{"urlkey":"photo","topic_name":"Photography"},{"urlkey":"creative","topic_name":"Creativity"},{"urlkey":"art","topic_name":"Art"},{"urlkey":"figuredrawing","topic_name":"Figure Drawing"},{"urlkey":"craftswap","topic_name":"Crafts"},{"urlkey":"sketching","topic_name":"Sketching"},{"urlkey":"painting","topic_name":"Painting"},{"urlkey":"social","topic_name":"Social"},{"urlkey":"acrylic-painting-classes","topic_name":"Acrylic Painting classes"},{"urlkey":"drawing-pencil-charcoal-pen-and-ink-markers","topic_name":"Drawing - Pencil, Charcoal, Pen and Ink, Markers"},{"urlkey":"painting-oil-acrylic-watercolor-etc","topic_name":"Painting - Oil, Acrylic, Watercolor, etc."},{"urlkey":"art-creating-art-and-perfecting-our-art","topic_name":"Art Creating Art and Perfecting Our Art"},{"urlkey":"painting-and-collage","topic_name":"Painting and collage"},{"urlkey":"watercolor-painting","topic_name":"Watercolor Painting"},{"urlkey":"figure-drawing-workshops","topic_name":"Figure Drawing Workshops"}],"group_lon":-78.61},"venue":{"lon":-78.606155,"venue_name":"ELEOS","venue_id":19111732,"lat":35.68585}}"""
  val rsvp = Util.jsonToObject(jsonRsvp, classOf[Rsvp])
  println(rsvp)

  val jsonGroup = """{"id":3967202,"name":"Active Lifestyle NERDS!","link":"http://www.meetup.com/es-ES/SoCalALN/","urlname":"SoCalALN","description":"<p>Greetings! Welcome to the <strong>Active Lifestyle Nerds</strong> Meetup community. This meetup was designed to bring out your <em>inner-nerd</em> and introduce it to other <em>inner-nerds</em> around your area---with a slight <strong><em>twist</em></strong>. Let\u2019s face it, the average person in a bar, club, or other common area where people socialize, often lacks the level of nerdism needed to have a good Fire Fly conversation or a discussion of \u201cwho shot first\u201d (it was definitely Han Solo, folks). <br> <br> This group is designed for the person that loves technology, Star Wars, comic books, Discovery Channel, the sciences, video games, or just about anything one would consider \u201cgeeky\u201d, but not necessarily willing to share it with their friends.&nbsp; <br> <br> <strong>This is especially true for you, <span>ladies</span>--</strong>often times the shiest and least willing to admit how into <strong>Naruto</strong> you really are. We want this to be a community where all \u201cnerds\u201d are accepted and given an opportunity to meet with other like-minded individuals. <br> <br> <strong><em>Here\u2019s the twist</em></strong>: <span><strong>Let\u2019s break that \u201cNerd\u201d stereotype and Assemble in non-nerdy locations throughout San Diego and beyond!</strong></span> <br> <br> <span>Ideally, anyone within the age range of 20-40 would benefit most from this group and overall experience but no one will be turned away!</span></p>\n<p>Come have new adventures and meet new friends at ALN!</p>\n<p><strong><em>Stay active, stay nerdy.</em></strong></p>","created":1338360176000,"city":"San Diego","country":"US","localized_country_name":"EE. UU.","state":"CA","join_mode":"approval","visibility":"public","lat":32.72,"lon":-117.17,"members":3992,"organizer":{"id":182698885,"name":"Active Lifestyle NERDS!","bio":"Welcome to the Active Lifestyle Nerds Meetup community!","photo":{"id":241769568,"highres_link":"http://photos4.meetupstatic.com/photos/member/7/3/8/0/highres_241769568.jpeg","photo_link":"http://photos2.meetupstatic.com/photos/member/7/3/8/0/member_241769568.jpeg","thumb_link":"http://photos2.meetupstatic.com/photos/member/7/3/8/0/thumb_241769568.jpeg"}},"who":"Active Nerds","group_photo":{"id":218230272,"highres_link":"http://photos1.meetupstatic.com/photos/event/2/8/2/0/highres_218230272.jpeg","photo_link":"http://photos1.meetupstatic.com/photos/event/2/8/2/0/600_218230272.jpeg","thumb_link":"http://photos1.meetupstatic.com/photos/event/2/8/2/0/thumb_218230272.jpeg"},"key_photo":{"id":450509583,"highres_link":"http://photos3.meetupstatic.com/photos/event/7/3/8/f/highres_450509583.jpeg","photo_link":"http://photos3.meetupstatic.com/photos/event/7/3/8/f/600_450509583.jpeg","thumb_link":"http://photos3.meetupstatic.com/photos/event/7/3/8/f/thumb_450509583.jpeg"},"timezone":"US/Pacific","next_event":{"id":"231921285","name":"BBQ for the 4th of July!","yes_rsvp_count":66,"time":1467648000000,"utc_offset":-25200000},"category":{"id":31,"name":"Vida social","shortname":"Social","sort_name":"Vida social"},"photos":[{"id":450509330,"highres_link":"http://photos3.meetupstatic.com/photos/event/7/2/9/2/highres_450509330.jpeg","photo_link":"http://photos3.meetupstatic.com/photos/event/7/2/9/2/600_450509330.jpeg","thumb_link":"http://photos3.meetupstatic.com/photos/event/7/2/9/2/thumb_450509330.jpeg"},{"id":450509497,"highres_link":"http://photos3.meetupstatic.com/photos/event/7/3/3/9/highres_450509497.jpeg","photo_link":"http://photos3.meetupstatic.com/photos/event/7/3/3/9/600_450509497.jpeg","thumb_link":"http://photos3.meetupstatic.com/photos/event/7/3/3/9/thumb_450509497.jpeg"},{"id":450509583,"highres_link":"http://photos3.meetupstatic.com/photos/event/7/3/8/f/highres_450509583.jpeg","photo_link":"http://photos3.meetupstatic.com/photos/event/7/3/8/f/600_450509583.jpeg","thumb_link":"http://photos3.meetupstatic.com/photos/event/7/3/8/f/thumb_450509583.jpeg"},{"id":450957770,"highres_link":"http://photos4.meetupstatic.com/photos/event/e/1/a/a/highres_450957770.jpeg","photo_link":"http://photos4.meetupstatic.com/photos/event/e/1/a/a/600_450957770.jpeg","thumb_link":"http://photos4.meetupstatic.com/photos/event/e/1/a/a/thumb_450957770.jpeg"}]}"""
  val group = Util.jsonToObject(jsonGroup, classOf[Group])
  println(group)

  val jsonEvent = """{"created":1467136382000,"id":"232229002","name":"Paco Erhard: 5-Step Guide to Being German","status":"upcoming","time":1467849600000,"updated":1467154711000,"utc_offset":-18000000,"waitlist_count":0,"yes_rsvp_count":20,"venue":{"id":1493847,"name":"Dank-Haus German Cultural Center","lat":41.968055725097656,"lon":-87.68907928466797,"repinned":false,"address_1":"4740 North Western Avenue","city":"Chicago","country":"us","localized_country_name":"EE. UU.","zip":"60625","state":"IL"},"group":{"created":1323287561000,"name":"Chicago Friends of German Culture","id":2918422,"join_mode":"open","lat":41.970001220703125,"lon":-87.69999694824219,"urlname":"chicagofriendsofgermanculture","who":"Germanicans"},"link":"http://www.meetup.com/es-ES/chicagofriendsofgermanculture/events/232229002/","description":"<p>Sold out at comedy festivals in Great Britain, Canada, and Australia, \"5-Step Guide to Being German\" is the result of the author's mission to set the record straight about his charming, crazy mishap of a country. Exploring the reality behind the stereotypes, award-winning German comedian Paco Erhard delivers an extremely clever analysis of the modern German psyche that is side-splittingly funny and violently politically incorrect. The show was nominated for the Fringe World Best Comedy Award in 2013.</p> <p>Paco Erhard shows audiences the Germany behind the scenes, as he deals with topics as varied as German bluntness (we just value honesty more highly than other peoples feelings), hilarious dilemmas on the Autobahn, the secret behind Lederhosen, and how it feels to have to grow up with Germany's horrible past.</p> <p>Food and drinks available for purchase</p> <p>Tickets are $10</p> <p><a href=\"http://www.brownpapertickets.com/event/2565394\" class=\"linkified\">http://www.brownpapertickets.com/event/2565394</a></p> <p><br/>Doors open an hour before the show starts</p> <p>Meet the Artist - Paco will stay after the show to give autographs and meet the audience.</p> ","visibility":"public"}"""
  val event = Util.jsonToObject(jsonEvent, classOf[Event])
  println(event)

  println(Util.objectToJson(event))


  val groupEvents = MeetupApi.getGroupPastEventsInfo("Clube-Poliglota-Rio-de-Janeiro")
  println(">>>>>>>>>>>>>>>>>>> <<<<<<<<<<<<<<<<<")
  if (groupEvents.isDefined) {
    val events = Util.jsonToObject(groupEvents.get, classOf[Array[Event]])
    println(events.length)
  }

}